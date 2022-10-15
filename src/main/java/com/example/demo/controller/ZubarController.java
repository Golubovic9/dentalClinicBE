package com.example.demo.controller;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.collections.Apointment;
import com.example.demo.repository.PregledRepository;
import com.example.demo.repository.ZubarRepository;

import model.Pregled;
import model.Zubar;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping(value="/dentist")
public class ZubarController {

	@Autowired
	ZubarRepository zubarRepo;
	
	@Autowired
	PregledRepository pregledRepo;
	
	
	@RequestMapping(value="/getWeekSchedule",method= RequestMethod.GET)
	public ResponseEntity<List<Apointment>> getWeekSchedule() {
		List<Pregled> sviPregledi = pregledRepo.findAll();
		List<Pregled> weekApointments = getWeekApointments(sviPregledi);
		List<Apointment> nedeljniPregledi = convertToApointments(weekApointments);
		return new ResponseEntity<List<Apointment>>(nedeljniPregledi, HttpStatus.OK);
	}
	
	private List<Pregled> getWeekApointments(List<Pregled> allApointments){
		List<Pregled> weekApointments = new ArrayList<Pregled>();
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		int currentWeek = c.get(Calendar.WEEK_OF_YEAR);
		int currentYear = c.get(Calendar.YEAR);
		for(Pregled p: allApointments) {
		    Calendar cal = Calendar.getInstance();
			cal.setTime(p.getDatum());
			int week = cal.get(Calendar.WEEK_OF_YEAR);
			int year = cal.get(Calendar.YEAR);
			if(week == currentWeek && year == currentYear) {
				weekApointments.add(p);
			}
		}
		return weekApointments;
	}
	
	@RequestMapping(value="/updateDeadline",method= RequestMethod.POST)
	public ResponseEntity<String> updateDeadline(@RequestBody Integer hours ) throws IOException {
		FileWriter fw = new FileWriter("/home/milos/workspace1/Ordinacija/deadline.txt");
    	BufferedWriter bw = new BufferedWriter(fw);
    	bw.write(hours.toString());
    	bw.close();
		return new ResponseEntity<String>("updated deadline", HttpStatus.OK);
	}
	
	@RequestMapping(value="/getSchedule", method= RequestMethod.POST)
	public String getSchedule(Date dan,Model m) throws ParseException {
		List<Pregled> pregledi = pregledRepo.findByDatum(dan);
		if(!pregledi.isEmpty()) {
			 List<Apointment> apointments = convertToApointments(pregledi);
			 Date datum = pregledi.get(0).getDatum();
			 String dayOfWeek = getDay(datum);
			 m.addAttribute("dayOfWeek", dayOfWeek);
			 m.addAttribute("pregledi", apointments);
		     m.addAttribute("datum", datum);
		}else {
			m.addAttribute("noApointmentInDay", true);
		}
		return "pocetnaZubar";
	}
	
	private List<Apointment> convertToApointments(List<Pregled> pregledi){
		List<Apointment> apointments = new ArrayList<Apointment>();
		for(Pregled p: pregledi) {
			 LocalTime start = p.getPocetak().toLocalTime().truncatedTo(ChronoUnit.MINUTES);
			 LocalTime kraj = p.getKraj().toLocalTime().truncatedTo(ChronoUnit.MINUTES);
			 Apointment a = new Apointment();
			 a.setDan(getDay(p.getDatum()));
			 a.setId(p.getIdPregled());
			 a.setPacijent(p.getPacijent());
			 a.setPocetak(start);
			 a.setKraj(kraj);
			 apointments.add(a);
		 }
		return apointments;
	}
	
	private String getDay(Date date) {
		 Calendar cal = Calendar.getInstance();
		 cal.setTime(date);
		 int day = cal.get(Calendar.DAY_OF_WEEK);
		 switch(day) {
		 case 1:
			 return "Nedelja";
		 case 2:
			 return "Ponedeljak";
		 case 3:
		 	return "Utorak";
		 case 4:
		 	return "Sreda";
		 case 5:
		 	return "Cetvrtak";
		 case 6:
			 return "Petak";
		 default :
			 return "Subota";
		 }
	}
	
	@RequestMapping(value="/cancel", method= RequestMethod.GET)
	public String cancel(Integer id,Model m) {
		m.addAttribute("id",id);
		return "unosKoda";
	}
	
	@RequestMapping(value="/login", method= RequestMethod.POST)
	public ResponseEntity<String> confirm(@RequestBody String id) {
		String response;
		Optional<Zubar> zubar = zubarRepo.findById(Integer.parseInt(id));
		if(zubar.isPresent()) {
			response = "fail";
		}else {
			response = "success";
		}
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}
	
	@RequestMapping(value="/confirmIdentity", method= RequestMethod.POST)
	public String confirm(Integer pregled,String kod , Model m,HttpServletRequest ht) {
		int idZubara;
		try {
			 idZubara = Integer.parseInt(kod);
		} catch (NumberFormatException e) {
			// TODO: handle exception
			m.addAttribute("notInt", true);
			m.addAttribute("id",pregled);
			return "unosKoda";
		}
		Optional<Zubar> zubar = zubarRepo.findById(idZubara);
		if(zubar.isPresent()) {
			Pregled apointmentToCancel = pregledRepo.findById(pregled).get();
			if(!checkTime(apointmentToCancel)) {
				m.addAttribute("past", true);
				return "pocetnaZubar";
			}
			pregledRepo.delete(apointmentToCancel);
			m.addAttribute("canceled", true);
			return "pocetnaZubar";
		}else {
			m.addAttribute("greska", true);
			m.addAttribute("id",pregled);
			return "unosKoda";
		}
		
		
	}
	
	private boolean checkTime(Pregled apointment) {
		Calendar calendar = Calendar.getInstance(); 
		calendar.setTime(new Date());  
		Date d = apointment.getDatum(); 
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.HOUR_OF_DAY,apointment.getPocetak().toLocalTime().getHour());
		cal.set(Calendar.MINUTE,apointment.getPocetak().toLocalTime().getMinute());
		if(cal.before(calendar)) {
			return false;
		}
		return true;
	}
}
