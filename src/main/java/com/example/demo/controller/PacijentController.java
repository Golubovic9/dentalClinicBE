package com.example.demo.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.collections.Apointment;
import com.example.demo.collections.Examination;
import com.example.demo.repository.PacijentRepository;
import com.example.demo.repository.PregledRepository;
import com.example.demo.repository.ZubarRepository;

import model.Pacijent;
import model.Pregled;
import model.Zubar;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping(value="/patient")
public class PacijentController {
	private LocalTime shiftStart = LocalTime.of(9, 00);
	private LocalTime shiftEnd = LocalTime.of(16, 30);

	@Autowired
	PacijentRepository pacijentRepo;
	
	@Autowired
	PregledRepository pregledRepo;
	
	@Autowired
	ZubarRepository zubarRepo;
	
	@RequestMapping(value="/getReservedApointments", method= RequestMethod.POST)
	public ResponseEntity<List<Apointment>> getReservedApointments(@RequestBody String id) throws NumberFormatException, IOException {
//		if(id.length()!=9 && id.length()!=10 ) {
//			m.addAttribute("wrongId", true);
//			return "cancelPage";
//		}
//		if(!id.matches("[0-9]+") ) {
//			m.addAttribute("wrongId", true);
//			return "cancelhomePage";
//		}
		int patientId = Integer.parseInt(id);
		Optional<Pacijent> patient = pacijentRepo.findById(patientId);
//		if(patient.isEmpty()) {
//			m.addAttribute("noSuchPatient", true);
//			return "cancelPage";
//		}
		List<Pregled> patientApointments = pregledRepo.findByPacijentIdPacijent(Integer.parseInt(id));
		Calendar calendar = Calendar.getInstance(); 
		calendar.setTime(new Date());               
		calendar.add(Calendar.HOUR_OF_DAY, getDeadline());      
		List<Pregled> nextApointments = new ArrayList<Pregled>();
		for(Pregled a :patientApointments ) {
			Date d = a.getDatum(); 
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			cal.set(Calendar.HOUR_OF_DAY,a.getPocetak().toLocalTime().getHour());
			cal.set(Calendar.MINUTE,a.getPocetak().toLocalTime().getMinute());
			cal.set(Calendar.SECOND,0);
			cal.set(Calendar.MILLISECOND,0);
			
			if(calendar.before(cal)) {
				nextApointments.add(a);	
			}
		}
		
		List<Apointment> pregledi = convertToApointments(nextApointments);
		return new ResponseEntity<List<Apointment>>(pregledi, HttpStatus.OK);
	}
	
	@RequestMapping(value="/cancelApointment/{id}", method= RequestMethod.DELETE)
	public ResponseEntity<String> getCancelPage(@PathVariable Integer id) {
		Pregled pregled = pregledRepo.findById(id).get();
		pregledRepo.delete(pregled);
		return new ResponseEntity<String>("uspesno otkazan pregled", HttpStatus.OK);
	}
	
	public int getDeadline() throws NumberFormatException, IOException {
		File fajl = new File("/home/milos/workspace1/Ordinacija/deadline.txt");
		BufferedReader br = new BufferedReader (new FileReader(fajl));
		String numberOfHours = br.readLine();
		br.close();
		int deadline = Integer.parseInt(numberOfHours);
		return deadline;
	}
	
	@RequestMapping(value="/getApointments", method= RequestMethod.POST)
	public ResponseEntity<List<Apointment>> getHomePage(@RequestBody String datum) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date dat = formatter.parse(datum);
		Calendar now = Calendar.getInstance();
		now.setTime(new Date());
		Calendar odabrani  = Calendar.getInstance();
		odabrani.setTime(dat);
		odabrani.set(Calendar.HOUR_OF_DAY, 17);
		odabrani.set(Calendar.MINUTE, 00);
//		if(now.after(odabrani)) {
//			String message = "uneli ste datum koji je prosao"; 
//			return "homePage";
//		}
		
		List<Pregled> pregledi = pregledRepo.findByDatum(dat);
//		if(pregledi.isEmpty()) {
//			return "homePage";
//		}
		List<Apointment> apointments = convertToApointments(pregledi);
	    //Date  date = pregledi.get(0).getDatum();
	    return new ResponseEntity<List<Apointment>>(apointments, HttpStatus.OK);
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
	
	private boolean validateApointment(Date apointmentDate, String start, Integer duration, String id,String email) {
		if(apointmentDate == null) {
			return false;
		}
		if(id.length()!=9 && id.length()!=10 ) {
			return false;
		}
		if(!id.matches("[0-9]+") ) {
			return false;
		}
		if(!email.contains("@")) {
			return false;
		}
		
		LocalTime pregledStart = LocalTime.parse(start);
		if(pregledStart.isBefore(shiftStart) || pregledStart.isAfter(shiftEnd)) {
			return false;
		}
		
		LocalTime pregledKraj;
		if(duration == 60)
			pregledKraj = pregledStart.plusMinutes(60);
		else
			pregledKraj = pregledStart.plusMinutes(30);
		if(!checkApointment(pregledStart,pregledKraj,apointmentDate)) {
			return false;
		}
		if(!checkPeriod(pregledStart)) {
			return false;
		}
		return true;
	}
	
//	@RequestMapping(value="/setApointment", method= RequestMethod.POST)
//	public ResponseEntity<String> setApointment(@RequestBody String apointmentDate, @RequestBody String start,@RequestBody Integer duration,@RequestBody String id,@RequestBody String email) throws ParseException {
//		String response;
//		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//		Date dat = formatter.parse(apointmentDate);
//		if(validateApointment(dat,start,  duration,  id, email)) {
//			response = "something went wrong";
//		}
//		Pacijent patient = savePatient(id,email);
//		LocalTime pregledStart = LocalTime.parse(start);
//		LocalTime pregledKraj;
//		if(duration == 60)
//			pregledKraj = pregledStart.plusMinutes(60);
//		else
//			pregledKraj = pregledStart.plusMinutes(30);
//		Time beggining = Time.valueOf(pregledStart); 
//		Time ending = Time.valueOf(pregledKraj);
//		
//		Pregled pregled = new Pregled();
//		pregled.setDatum(dat);
//		pregled.setPacijent(patient);
//		pregled.setPocetak(beggining);
//		pregled.setKraj(ending);
//		Pregled savedApointment = pregledRepo.save(pregled);
//		List<Zubar> zubar = zubarRepo.findAll();
//		response = "pregled uspesno zakazan";
//		return new ResponseEntity<String>(response, HttpStatus.OK);
//	}
	
	@RequestMapping(value="/setApointment", method= RequestMethod.POST)
	public ResponseEntity<String> setApointment(@RequestBody Examination e) throws ParseException {
		String response;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//		Date dat = formatter.parse(e.getApointmentDate());
		if(!validateApointment(e.getApointmentDate(), e.getStart(),  e.getDuration(),  e.getId(), e.getEmail())) {
			response = "something went wrong";
			return new ResponseEntity<String>(response, HttpStatus.OK);
		}
		Pacijent patient = savePatient(e.getId(),e.getEmail());
		LocalTime pregledStart = LocalTime.parse(e.getStart());
		LocalTime pregledKraj;
		if(e.getDuration() == 60)
			pregledKraj = pregledStart.plusMinutes(60);
		else
			pregledKraj = pregledStart.plusMinutes(30);
		Time beggining = Time.valueOf(pregledStart); 
		Time ending = Time.valueOf(pregledKraj);
		
		Pregled pregled = new Pregled();
		pregled.setDatum(e.getApointmentDate());
		pregled.setPacijent(patient);
		pregled.setPocetak(beggining);
		pregled.setKraj(ending);
		Pregled savedApointment = pregledRepo.save(pregled);
		List<Zubar> zubar = zubarRepo.findAll();
		response = "pregled uspesno zakazan";
		return new ResponseEntity<String>(response, HttpStatus.OK);
	}
	
	private Pacijent savePatient(String id,String email) {
		int patientId = Integer.parseInt(id);
		Optional<Pacijent> patient = pacijentRepo.findById(patientId);
		if(patient.isPresent()) {
			return patient.get();
		}else {
			Pacijent pacijent = new Pacijent();
			pacijent.setIdPacijent(patientId);;
			pacijent.setEmail(email);
			pacijentRepo.save(pacijent);
			Pacijent ret = pacijentRepo.findById(patientId).get();
			return ret;
		}
		
	}
	
	private boolean checkApointment(LocalTime start,LocalTime end,Date date) {
		List<Pregled> apointments = pregledRepo.findByDatum(date);
		for(Pregled a : apointments) {
			LocalTime pocetak = a.getPocetak().toLocalTime();
			LocalTime kraj = a.getKraj().toLocalTime();
			if(pocetak.equals(start) || kraj.equals(end)){
				return false;
			}
			if(pocetak.isAfter(start) && pocetak.isBefore(end)) {
				return false;
			}
			if(kraj.isAfter(start) && kraj.isBefore(end)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean checkPeriod(LocalTime start) {
		if(start.getMinute()==30 || start.getMinute()==0) {
			return true;
		}
		
		return false;
	}
	
}
