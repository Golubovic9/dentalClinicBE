package com.example.demo.collections;

import java.time.LocalTime;

import model.Pacijent;

public class Apointment {
	private int id;
	private String dan;
	private LocalTime pocetak;
	private LocalTime kraj;
	private Pacijent pacijent;
	
	
	public Apointment() {
		
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getDan() {
		return dan;
	}
	public void setDan(String dan) {
		this.dan = dan;
	}
	public LocalTime getPocetak() {
		return pocetak;
	}
	public void setPocetak(LocalTime pocetak) {
		this.pocetak = pocetak;
	}
	public LocalTime getKraj() {
		return kraj;
	}
	public void setKraj(LocalTime kraj) {
		this.kraj = kraj;
	}
	public Pacijent getPacijent() {
		return pacijent;
	}
	public void setPacijent(Pacijent pacijent) {
		this.pacijent = pacijent;
	}
}
