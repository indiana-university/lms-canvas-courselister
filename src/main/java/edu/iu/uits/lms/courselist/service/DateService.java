package edu.iu.uits.lms.courselist.service;

import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DateService {

   public Date getCurrentDate() {
      return new Date();
   }
}
