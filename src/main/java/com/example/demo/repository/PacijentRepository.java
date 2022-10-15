package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import model.Pacijent;

public interface PacijentRepository extends JpaRepository<Pacijent,Integer>{

}
