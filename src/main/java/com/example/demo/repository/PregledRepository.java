package com.example.demo.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Pregled;

public interface PregledRepository extends JpaRepository<Pregled,Integer> {
	public List<Pregled> findByDatum(Date date);
	public List<Pregled> findByPacijentIdPacijent(int id);
}
