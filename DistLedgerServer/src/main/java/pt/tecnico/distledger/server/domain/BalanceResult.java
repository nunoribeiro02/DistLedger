package pt.tecnico.distledger.server.domain;

import java.util.List;

public class BalanceResult {

   private List<Integer> ts;

   private Integer balance;

   //Getters and Setters
   public void setTS(List<Integer> ts) {
    this.ts=ts;
   }

   public List<Integer> getTS() {
    return this.ts;
   }

   public void setBalance(Integer balance) {
    this.balance=balance;
   }
    
   public Integer getBalance() {
    return this.balance;
   } 
}



