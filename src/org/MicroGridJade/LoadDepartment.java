package org.MicroGridJade;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.IOException;
import java.util.*;

public class LoadDepartment extends Agent{


//public LoadDepartment consumer=this;

    private float[] demandValue;
    private boolean end;
    private AID[] powerManager;
    public int j=0;// Auxiliar variable that serve to instance DemandRequest Behavoiur or finishing the Agent
    private String[] demandValue_Str;

    /* Attribute that must determine the seconds every when it sends a demand point, it is defined in 1800s
        Although this value must be taken from a textBox filled in by the user in a form */
    public float ti=1800;

    protected void setup(){

        System.out.println("Welcome consumer "+this.getName()+".");

        demandValue=new float[24];
        demandValue_Str=new String[24];

        demandValue[0]=100;
        demandValue[1]=100;
        demandValue[2]=100;
        demandValue[3]=100;
        demandValue[4]=100;
        demandValue[5]=200;
        demandValue[6]=300;
        demandValue[7]=400;
        demandValue[8]=600;
        demandValue[9]=700;
        demandValue[10]=900;
        demandValue[11]=1000;
        demandValue[12]=900;
        demandValue[13]=1000;
        demandValue[14]=1300;
        demandValue[15]=1500;
        demandValue[16]=1300;
        demandValue[17]=1200;
        demandValue[18]=1000;
        demandValue[19]=900;
        demandValue[20]=700;
        demandValue[21]=300;
        demandValue[22]=200;
        demandValue[23]=100;

        //It's convert demandValue to String
        for(int i=0; i<demandValue.length;i++){
            demandValue_Str[i]=Float.toString(demandValue[i]);
        }


        //It's register in other to be localizated by Power Manager and receiving the confirmation the demand point
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("LoadRequestDepartment-PM");
        sd.setName("LoadRequest-PM JADE");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }catch (FIPAException fe) {
            fe.printStackTrace();
        }


        addBehaviour(new LoadDepartment.SearchPM());


    }


    private class SearchPM extends Behaviour{

        private int count;
        DFAgentDescription[] result1=new DFAgentDescription[1];

        public void action(){

            //Search the PM
            DFAgentDescription template = new DFAgentDescription();//DF template to generators
            ServiceDescription sd = new ServiceDescription();  //Service description to generators
            sd.setType("controlAgent-PM");
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                result1=result;
                if(result.length>0){

                    System.out.println("There are the following Control Agent Power Managers:");
                    powerManager = new AID[result.length];

                    for (int i = 0; i < result.length; ++i) {

                        powerManager[i] = result[i].getName();
                        System.out.println(powerManager[i].getName());
                    }
                    myAgent.addBehaviour(new LoadDepartment.DemandRequest());
                }else{
                    System.out.println("Waiting for Control Agent ... ");
                    //myAgent.doSuspend();
                    block();
                    myAgent.doWait(20000);
                  /*  count++;
                    if (count==8){
                      myAgent.doDelete();
                    }*/

                }
            }catch (FIPAException fe) {
                fe.printStackTrace();
            }


        }
        public boolean done(){
            end=true;
            return (result1.length>0);
        }
    }

    private class DemandRequest extends Behaviour{

        private AID pm_id[];
        private MessageTemplate mt;


        public void action(){

            // Define the type of message
            ACLMessage request= new ACLMessage(ACLMessage.REQUEST);

            for (int i = 0; i < powerManager.length; ++i) {
                request.addReceiver(powerManager[i]);
            }

            //It's convert demandValue to String
            //for(int i=0; i<demandValue.length;i++){
            //demandValue_Str[i]=Float.toString(demandValue[i]);
            //  }

            //It's sent the demanded power
            pm_id=new AID[powerManager.length];
            request.setContent(demandValue_Str[j]);
            request.setConversationId("D_PM");
            request.setReplyWith("request"+System.currentTimeMillis()); // Unique value
            System.out.println("The consumer "+myAgent.getName()+" send Request: "+request.getContent());
            myAgent.send(request);
            j++;

            if(j>0 && j<demandValue.length){
                addBehaviour(new LoadDepartment.PM_Commit());
            }
        }

        public boolean done(){
            end=true;
            return end;
        }
    }

    private class PM_Commit extends CyclicBehaviour{

        private MessageTemplate mt;

        public void action(){

            mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            ACLMessage commit_demand=myAgent.receive(mt);

            if(commit_demand!=null){

                System.out.println("\n\n\n\nThe consumer "+myAgent.getName()+" has received confirmation "+commit_demand.getContent()+"\n\n\n\n");

                if(j<demandValue.length){

                    addBehaviour(new LoadDepartment.DemandRequest());
                }
                if(j>=demandValue.length){
                    // Define the type of message
                    ACLMessage finish= new ACLMessage(ACLMessage.CANCEL);

                    System.out.println("\nThe consumer "+myAgent.getName()+" is finished");

                    for (int i = 0; i < powerManager.length; ++i) {
                        finish.addReceiver(powerManager[i]);
                    }
                    finish.setContent("Last demand point");
                    finish.setConversationId("Finish process");
                    finish.setReplyWith("Finish"+System.currentTimeMillis());
                    System.out.println("The consumer send finish order to Power Manager \n");
                    myAgent.send(finish);
                    doDelete();
                }

            }else{
                block();
            }
        }
    }
}
