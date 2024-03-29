/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.MicroGridJade;

/** @author endryys*/
public class BatteryAction extends Battery {
    
    //Attributes: The idea is this attributes are filled by user by textbox in forms
    
   /* public float p_nominal_=(float)149.46;
    public float capacity_=1000;
    public float soc_=35;*/
    
    public float p_nominal_;
    public float capacity_;
    public float soc_;
    
    public float soc_max=98;
    public float soc_min=5;
    public float soc_upper=40;
    public float soc_lower=30;
    
    /*private Battery battery;
    private Battery_GUI batt_GUI;*/
    
   
    //Methods
    
    
    public void CatchBatteryData(float capacity, float p_nominal, float soc){
        
        this.capacity_=capacity;
        this.p_nominal_=p_nominal;
        this.soc_=soc;
        
    }
    
    public float ChargingStatus(float power,float interval,float soc){
        float soc_output;
        float p_charge,e_charge,e_capacity,e_available;
        
        p_charge=power;
        e_charge=p_charge*interval;
        e_capacity=this.capacity_*3600;
        e_available=(this.soc_max-soc)*e_capacity/100;
        soc_output=(1+(e_charge/(e_capacity-e_available)))*soc;
        float margin=3/10;
        //soc_output=soc_output+ margin;
        return soc_output;
    }
    
    public float DischargingStatus(float power,float interval,float soc){
        float soc_output;
        float p_discharge,e_discharge,e_capacity,e_available;
        
        p_discharge=power;
        e_discharge=p_discharge*interval;
        e_capacity=this.capacity_*3600;
        e_available=(soc-this.soc_min)*e_capacity/100;
        soc_output=(1-(e_discharge/e_available))*soc;
        //soc_output=soc_output+1;
        return soc_output;
    }  
    
    public float PowerDischargingTillSOCmin(float interval,float soc){
        
        float p_batt_output=0;
        float e_capacity,e_available;
        
        e_capacity=this.capacity_*3600;
        e_available=(soc-this.soc_min)*e_capacity/100;
        p_batt_output=(1-(this.soc_min/soc))*(e_available/interval);
        return p_batt_output;
    }
    
    public float PowerChargingTillSOCmax(float interval,float soc){
        
        float p_batt_output=0;
        float e_capacity,e_available;
        
        e_capacity=this.capacity_*3600;
        e_available=(this.soc_max-soc)*e_capacity/100;
        p_batt_output=(((this.soc_max/soc)-1)*(e_capacity-e_available))/interval;
        return p_batt_output;
    }

    
    public float[] Battery_Information(float p_diff, float status_, float threshold_,float soc_, String agentBattery_name,float interval){
    
        float [] batt_output=new float[3];
    
        int status=0,threshold=0;
        float p_charge,p_discharge;

        float p_batt_output_=0;
        float p_batt_max=0;
        float aCD;
        boolean charge=false, discharge=false;
       
        if(status_==1.0){status=1;}
        if(status_==2.0){status=2;}
        if(threshold_==-1.0){threshold=-1;}
        if(threshold_==0.0){threshold=0;} 
        if(threshold_==1.0){threshold=1;} 
    
        //Status 3 enters only when soc is not outside the limits of soc_lower and soc_upper
        //and when the pcc_initial is within the limits of the PeakShaving
        if(status_==3.0){
            status=3;
            if (soc_<soc_lower){
                          
                charge=true;
                discharge=false;
            }
            if(soc_>soc_upper){
                           
                discharge=true;
                charge=false;
            }
                        
            if(soc_>=soc_lower && soc_<=soc_upper){
                               
                charge=false;
                discharge=false;
            }
        }
        
      
                    
            switch(status){
                        
                        case 1: //Battery deliver energy
                            aCD=0;
                            
                            //The maximum power it can deliver is calculated
                            p_batt_max=this.PowerDischargingTillSOCmin(interval, soc_);
                            
                            if(soc_<=soc_min){
                
                                System.out.println("The Battery "+agentBattery_name+" is too low, cannot deliver power\n.");
                                p_batt_output_=0;
                                batt_output[0]=p_batt_output_;
                                batt_output[1]=soc_;
                                batt_output[2]=aCD;
                                
                            }else{
                                
                                // if(p_diff>=p_nominal_){
                                    
                                //     if(p_nominal_>=p_batt_max){
                                        
                                //         p_batt_output_=p_batt_max;
                                //         batt_output[0]=p_batt_output_;
                                //         batt_output[1]=soc_;
                                //         batt_output[2]=aCD;
                                        
                                //     }else{
                                        
                                //         p_batt_output_=p_nominal_;
                                //         batt_output[0]=p_batt_output_;
                                //         batt_output[1]=soc_;
                                //         batt_output[2]=aCD;
                                //     }
                                    
                                // }else{//If p_diff is less than p_nominal


                                    if(p_batt_max<p_diff){
                                        
                                        p_batt_output_=p_batt_max;
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;
                                        
                                    }else{
                                        
                                        p_batt_output_=p_diff;
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;
                                    }
                                    
                                }
                            //}
                            
                            break;
                            
                        case 2://Battery consume energy
                            aCD=0;
                            
                            //The maximum power it can absorb is calculated
                            p_batt_max=this.PowerChargingTillSOCmax(interval, soc_);
                            
                            if(soc_>=soc_max){
                                
                                System.out.println("The Battery "+agentBattery_name+" has too high a level, cannot absorb more energy\n.");
                                p_batt_output_=0;
                                batt_output[0]=p_batt_output_;
                                batt_output[1]=soc_;
                                batt_output[2]=aCD;
                                
                            }else{
                                
                                p_diff=-p_diff;

                                // if(p_diff>=p_nominal_){
                                    
                                //     if(p_nominal_>=p_batt_max){
                                        
                                //         p_batt_output_=p_batt_max;
                                //         batt_output[0]=p_batt_output_;
                                //         batt_output[1]=soc_;
                                //         batt_output[2]=aCD;
                                        
                                //     }else{
                                        
                                //         p_batt_output_=p_nominal_;
                                //         batt_output[0]=p_batt_output_;
                                //         batt_output[1]=soc_;
                                //         batt_output[2]=aCD;
                                //     }
                                 
                                // }else{ //Si p_diff es inferior a la p_nominal
                                    
                                    if(p_batt_max<p_diff){
                                        
                                        p_batt_output_=p_batt_max;
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;
                                        
                                    }else{
                                        
                                        p_batt_output_=p_diff;
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;   
                                    }
                                }
                            //}    
                            
                            break;
                            
                        case 3://Battery automatic charge/discharge
                            
                            if(charge==true){
                                
                                //System.out.println("The Battery enters the automatic charging phase");
                                
                                if(threshold==0 || threshold==-1){
                                    
                                    p_charge=(p_nominal_+p_nominal_*((soc_lower-soc_)/(soc_lower-soc_min)))/2;
                                    
                                    if(p_charge<=p_diff){
                                        
                                        aCD=1;
                                        
                                        if(p_charge<0){p_charge=-p_charge;}   
                                        
                                            p_batt_output_=p_charge;
                                            System.out.println("\nThe Battery "+agentBattery_name+" can absorb a power: "+p_batt_output_);
                                            batt_output[0]=p_batt_output_;
                                            batt_output[1]=soc_;
                                            batt_output[2]=aCD;
                                            //p_batt_output is left at zero, since in this case it should not consume power from the system since it is within the limits
                                            //p_batt_output_=0;
                                    }else{
                                        
                                        p_charge=p_diff;
                                        aCD=1;
                                        
                                        if(p_charge<0){p_charge=-p_charge;}
                                        
                                            p_batt_output_=p_charge;
                                            System.out.println("\nThe Battery"+agentBattery_name+" can absorb a power: "+p_batt_output_);
                                            batt_output[0]=p_batt_output_;
                                            batt_output[1]=soc_;
                                            batt_output[2]=aCD;
                                    }
                                    
                                }else{
                                    
                                    p_batt_output_=0;
                                    aCD=0;
                                    batt_output[0]=p_batt_output_;
                                    batt_output[1]=soc_;
                                    batt_output[2]=aCD;
                                }
                            }
                            if(discharge==true){
                                
                                //System.out.println("La batería entraría en fase de descarga automática");
                                
                                if(threshold==0 || threshold==1){
                                    
                                    p_discharge=(p_nominal_+p_nominal_*((soc_-soc_upper)/(soc_max-soc_upper)))/2;
                                    
                                    if(p_discharge<=p_diff){
                                        
                                        aCD=-1;
                                        if(p_discharge<0){p_discharge=-p_discharge;} 
                                        p_batt_output_=p_discharge;
                                        System.out.println("\nThe Battery "+agentBattery_name+" would yield a power: "+p_batt_output_);
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;
                                        //P_batt_output is left at zero, since in this case it should not deliver system power since it is within the limits
                                        //p_batt_output_=0;
                                        
                                    }else{
                                        
                                        p_discharge=p_diff;
                                        aCD=-1;    
                                        if(p_discharge<0){p_discharge=-p_discharge;} 
                                        p_batt_output_=p_discharge;
                                        System.out.println("\n The Battery "+agentBattery_name+" would yield a power: "+p_batt_output_);
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;                                        
                                    }
                                    
                                }else{
                                    
                                    p_batt_output_=0;
                                    aCD=0;
                                    batt_output[0]=p_batt_output_;
                                    batt_output[1]=soc_;
                                    batt_output[2]=aCD;                                    
                                }                                
                            }
                            //Efficient loading and unloading phase
                            /*This phase enters when the current SOC of the battery is within the established ranges, which does not have the need for automatic charge / discharge.
                            and can contribute or consume from the system to further dampen the Pcc_final curve.*/
                            if(charge==false && discharge==false){
                               
                                if (p_diff!=0){
                                    
                                    if(p_diff>0){
                                        
                                        p_discharge=p_diff;
                                        aCD=0;
                                        p_batt_output_=p_discharge;  
                                        System.out.println("The Battery "+agentBattery_name+" would yield :"+p_batt_output_);
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;
                                        
                                    }else{
                                        
                                        p_charge=p_diff;
                                        p_batt_output_=p_charge;
                                        aCD=0;
                                        System.out.println("The Battery "+agentBattery_name+" would absorb :"+p_batt_output_);
                                        batt_output[0]=p_batt_output_;
                                        batt_output[1]=soc_;
                                        batt_output[2]=aCD;
                                    }
                                    
                                }else{
                                    
                                    p_batt_output_=0;
                                    aCD=0;
                                    batt_output[0]=p_batt_output_;
                                    batt_output[1]=soc_;
                                    batt_output[2]=aCD;
                                
                                }
                            }
                            break;        
                    }
    return batt_output;
    }
}
