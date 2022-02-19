package org.MicroGridJade;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;

import javax.sound.sampled.SourceDataLine;

public class CentralControlAgent extends Agent  {



    // controlAgent details
    private String powerDemand_Str;
    private float powerDemand;
    private String pm_name;
    private int a=0;// Auxiliary variable for storing the values ​​of the arrays
    private  int count=0;

    String powerDemand_hostel_Str;
    String powerDemand_department_Str;

    float powerDemand_hostel;
    float powerDemand_department;


    float powerGenerated_hostel;
    float powerGenerated_department;
    float powerGenerated_Total=0;

    float powerGenerated_hostel_wind;
    float powerGenerated_department_wind;
    float powerGenerated_hostel_solar;
    float powerGenerated_department_solar;

    String hostelCredit_str="";
    String hostelDebit_str="";
    String departmentDebit_str="";
    String departmentCredit_str="";

    float hostelCredit=0;
    float hostelDebit=0;
    float departmentDebit=0;
    float departmentCredit=0;

    float final_check=0;
    // The list of known generator agents
    private AID[] AgentConsumers;
    private AID[] AgentsGenerators;
    private AID[] AgentsBatteries;
    private String[][] mLoads;
    private AID[] aidLoads;


    private String p_battery_Str;
    private String pbat_total;

    private String pcc_final_Str;
    private String pfinal;

    public String[] a_pcc_initial=new String[48];
    public String[] a_pcc_final=new String[48];
    public String[] a_p_batt_total=new String[48];

    int hourOfDay=0;

    private ACLMessage demand_point=new ACLMessage();
    private float pDemand =0;

    // Put agent initializations here
    protected void setup() {

        // Printout a welcome message
        System.out.println("Hi there! Central Control Agent  " + getAID().getName());

        //It's register and added the service to consumer's demand
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("controlAgent-PM");
        sd.setName("controlAgent JADE");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new CentralControlAgent.IdentifyLoads());

    }
        protected void takeDown() {
            // Printout a dismissal message
            System.out.println("Agent-Powermanager "+getAID().getName()+
                    " Terminated.");
    }

    private class IdentifyLoads extends Behaviour {

        DFAgentDescription[] consumers= new  DFAgentDescription[48];

        public void action(){

            int i=0;
            int count=0;
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("LoadRequestHostel-PM");
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                consumers=result;

                if(result.length>0 ){

                    System.out.println("The following consumer agents are found: ");
                    AgentConsumers = new AID[result.length+1];
                    for (i = 0; i < result.length+1; ++i) {
                        AgentConsumers[i] = result[0].getName();
                        System.out.println(AgentConsumers[i].getName());
                        count=count+1;
                        if(count>0)
                        {
                            sd.setType("LoadRequestDepartment-PM");
                            template.addServices(sd);
                            result = DFService.search(myAgent, template);
                        }
                    }

                    if(count>0) {
                        mLoads = new String[result.length+1][2];
                        aidLoads = new AID[result.length+1];
                        AgentConsumers = new AID[result.length+1];
                    }else
                    {
                        mLoads = new String[result.length][2];
                        aidLoads = new AID[result.length];
                        AgentConsumers = new AID[result.length];
                    }
                    addBehaviour(new CentralControlAgent.ReceiveLoadRequests());

                }else{

                    System.out.println("No consumer was contacted.\n ");
                    System.out.println(
                            "No consumers found, Please add some consumers and re-create a Power Manager agent. \n");
                    //myAgent.doDelete();
                    block();
                    myAgent.doWait(20000);

                }

            }catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
        public boolean done(){
            return(consumers.length>0);
        }

    }
    private class ReceiveLoadRequests extends CyclicBehaviour {

        private MessageTemplate mt; // The template to receive replies
        private int j=0;
        private int i=0;
        public void action(){

            String pm_id= new String();
            pm_id=myAgent.getName();
            String[] a_pm_id=pm_id.split("@");
            pm_name=a_pm_id[0];

            mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

            demand_point = myAgent.receive(mt);

            if (demand_point!=null){

                mLoads[i][0]=demand_point.getSender().getName();
                mLoads[i][1]=demand_point.getContent();
                aidLoads[i]=demand_point.getSender();
                powerDemand_Str = demand_point.getContent();


                if(mLoads[0][i].contains("Hostel"))
                {
                    powerDemand_hostel_Str=demand_point.getContent();
                    powerDemand_hostel=Float.parseFloat(powerDemand_hostel_Str);
                }
                else
                {
                    powerDemand_department_Str=demand_point.getContent();
                    powerDemand_department=Float.parseFloat(powerDemand_department_Str);
                }
                i++;

                if(j>= AgentConsumers.length-1){

                    pDemand = pDemand +Float.parseFloat(powerDemand_Str);
                    powerDemand_Str=Float.toString(pDemand);
                    myAgent.addBehaviour(new CentralControlAgent.GenerationBehaviour());
                    j=0;
                    i=0;
                    //d_point++;

                }else{ //If it is less than the number of consumer agents

                    pDemand = pDemand +Float.parseFloat(powerDemand_Str);
                    j++;
                }

            }else{
                block();//It's blocked until demand_point was different null
            }
        }

    }
    /*Behaviour that initialize the operation*/
    private class GenerationBehaviour extends Behaviour{

        boolean fin=false;
       // DFAgentDescription[] generators=new DFAgentDescription[48];

        int generatorsLength=0;

        public void action(){
            System.out.println("I need a generating power equal to"+ pDemand);

            // Update the list of generators agents
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("demand-generationHostel");
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
               // generators=result;
                if(result.length>0){
                    System.out.println("The following generating agents are found:");
                    AgentsGenerators = new AID[result.length+2];
                    for (int i = 0; i < result.length; ++i) {
                        AgentsGenerators[i] = result[i].getName();
                        System.out.println(AgentsGenerators[i].getName());
                    }
                    if(AgentConsumers.length>1)
                    {
                        sd.setType("demand-generationDepartment");
                        template.addServices(sd);
                        result = DFService.search(myAgent, template);
                        generatorsLength=4;
                    }
                    for (int j = 2; j < result.length+2; ++j) {
                        AgentsGenerators[j] = result[j-2].getName();
                        System.out.println(AgentsGenerators[j].getName());
                    
                    }
                    
                    myAgent.addBehaviour(new CentralControlAgent.PurchaseRequest());
                }else{
                    System.out.println("Waiting for generators ... ");
                    block();
                    myAgent.doWait(20000);

                }

            }catch (FIPAException fe) {
                fe.printStackTrace();
            }

        }
        public boolean done() {
            fin=true;
            return (generatorsLength>0);
        }

    }

    /**Inner class RequestPerformer.
     This is the behaviour used by Book-buyer agents to request seller
     agents the target book.*/
    private class PurchaseRequest extends Behaviour {

        private AID bestSeller; // The agent who provides the best offer
        private AID bestOption_batt;
        private int bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt;
        private MessageTemplate mt1;
        private MessageTemplate mt2;
        private MessageTemplate mt3;// The template to receive replies
        private int step = 0;
        private float powerGenerated;
        private float powerBattery;//Variable global to accumulate battery power

        private float pBatts=0;
        private AgentFeatures [] agentsG1,agentsG2,agentsG3,agentsG4;
        private AgentFeatures [] agentsB1,agentsB2;
        //private AgentFeatures [][] matrix2;
        private PowerSelector powerSelect= new PowerSelector();
        private int j=0;
        private int k=0;
        private String pcc_initial,pcc_final;
        private String p_diff,status,threshold;
        private int numOrderBatt;
        private int numreplybatt=0;
        private float _aCD=0;


        private AgentFeatures[] agentBatts;
        private PowerSelector  battData=new PowerSelector();


        DFAgentDescription[] dfbatts = new DFAgentDescription[48];

        public void action() {

            int i;
            int n = AgentsGenerators.length;
            int n_batt;
            AID gen;
            AID batt;
            AID[] generator_id, battery_id, consumer_id, department_id;
            float p_generated, price;
            float price_ = 0;
            float priceS = 0;
            float pBat = 0;
            float powerGenerada;
            float powerBaterias;
            int numOrderDemand;
            int numOrderBatt;
            //int numreplybatt=0;

            float pcc_initial_, pcc_final_, p_batt, soc_batt, pcc_initial_0, pcc_initial_1;
            float aCD_ = 0;//Automatic Charge and Discharge
            int aCD = 0;//Automatic Charge and Discharge
            String batt_input, batt_propose, batt_output;

            switch (step) {

                case 0:

                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (i = 0; i < AgentsGenerators.length; ++i) {
                        cfp.addReceiver(AgentsGenerators[i]);
                    }
                    //p_generations=new int[AgentsGenerators.length];
                    generator_id = new AID[AgentsGenerators.length];
                    cfp.setContent(powerDemand_Str);
                    cfp.setConversationId("G_PM");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    System.out.println(" Central control agent sends CFP: " + cfp.getContent());
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("G_PM"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        System.out.println("Central control agent receives a proposal from: " + reply.getContent());
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            //Treatment of the proposal received
                            String arr = reply.getContent();
                            System.out.println(arr);
                            String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");


                            float[] results = new float[items.length];


                            for (i = 0; i < items.length; i++) {
                                try {
                                    results[i] = Float.parseFloat(items[i]);
                                } catch (NumberFormatException nfe) {
                                }
                            }

                            p_generated = results[0];
                           // price = results[1];
                            i = 0;
                            gen = reply.getSender();
                            System.out.println(gen);

                            //Sources are storage
                            if (j < n) {
                                if (gen.toString().contains("Hostel")) {

                                    powerGenerated_hostel = p_generated+powerGenerated_hostel;
                                    if(gen.toString().contains("Wind"))
                                    {
                                        powerGenerated_hostel_wind=p_generated;
                                        AgentsGenerators[1]=gen;
                                    }
                                    else{
                                        powerGenerated_hostel_solar=p_generated;
                                        AgentsGenerators[0]=gen;
                                    }


                                } else {
                                    powerGenerated_department = p_generated+powerGenerated_department;
                                    
                                    if(gen.toString().contains("Wind"))
                                    {
                                        powerGenerated_department_wind=p_generated;
                                        AgentsGenerators[3]=gen;
                                    }
                                    else{
                                        powerGenerated_department_solar=p_generated;
                                        AgentsGenerators[2]=gen;
                                    }

                                }
                                
                                j++;
                            }
                            //When all sources have been storage
                            if (j == n) {
                                powerGenerated_Total = powerGenerated_hostel + powerGenerated_department;
                                if (powerDemand_hostel == powerGenerated_hostel) {
                                    System.out.println("Total load requested in hostel is same as power generated from solar and wind hostel --> " + "hostel Load Demand: " + powerDemand_hostel + " , hostel solar and wind generation: " + powerGenerated_hostel);
                                } else if (powerDemand_hostel > powerGenerated_hostel) {
                                    System.out.println("Total load requested in hostel is greater than as power generated from solar and wind hostel generators--> " + "hostel Load Demand: " + powerDemand_hostel + " , hostel solar and wind generation: " + powerGenerated_hostel);
                                    System.out.println("\n check if department solar has surplus power generation \n ");

                                } else if (powerDemand_hostel < powerGenerated_hostel) {
                                    System.out.println("Total load requested in hostel is less than as power generated from solar and wind hostel--> " + "hostel Load Demand: " + powerDemand_hostel + " , hostel solar and wind generation: " + powerGenerated_hostel);
                                    System.out.println("\n check if department load needs power or else store the power into battery where SOC% is lesser \n");
                                }
                                if (powerDemand_department == powerGenerated_department) {
                                    System.out.println("Total load requested in department is same as power generated from solar and wind department --> " + "department Load Demand: " + powerDemand_department + " , department solar and wind generation: " + powerGenerated_department);
                                } else if (powerDemand_department > powerGenerated_department) {
                                    System.out.println("Total load requested in department is greater than as power generated from solar and wind department--> " + "department Load Demand: " + powerDemand_department + " , department solar and wind generation: " + powerGenerated_department);
                                    System.out.println("\n check if hostel solar has surplus power generation\n");

                                } else if (powerDemand_department < powerGenerated_department) {
                                    System.out.println("Total load requested in department is less than as power generated from solar and wind department--> " + "department Load Demand: " + powerDemand_department + " , department solar and wind generation: " + powerGenerated_department);
                                    System.out.println("\n check if hostel load needs power or else store the power into battery where SOC% is lesser \n");

                                }
                            }

//                        //Sources are storage
//                        if(j<n){
//                            agentsG1=powerSelect.DataStorage(n,gen,p_generated,price);
//                            j++;
//                        }
//                        //When all sources have been storage
//                        if(j==n){
//
//                            //It's calculated price_mean of energy
//                            for(i=0; i<n;i++){
//                                priceS=agentsG1[i].GetArrayAgent_p_price();
//                                price_=price_+priceS;
//                            }
//                            float price_mean=price_/n;
//                            //System.out.println("The average price of the generated power is: "+price_mean);
//                            //Sources are classified according their power and prices
//                            agentsG2=powerSelect.DataOrganizer(n, agentsG1, price_mean);
//                            bestSeller=agentsG2[0].GetArrayAgent_AID();
                            //}

                        }
                        repliesCnt++;
                                        /*If the number of responses is equal to or greater than the number of registered generators
                                          means that you have received all proposals from all generators
                                          and you can move on to the next stage.*/
                        if (repliesCnt >= AgentsGenerators.length) {
                            // We received all replies
                            step = 2;
                            repliesCnt=0;
                            break;

                        }
                    } else {
                        block();
                    }
                    break;

                case 2:
                    // Send the purchase order to the Generators both

                    StrategyControl p_threshold = new StrategyControl();
                    System.out.println("Hostel Load : " + powerDemand_hostel_Str);
                    System.out.println("Hostel Solar + wind Generation : " + powerGenerated_hostel);
                    System.out.println("Hostel Solar Generation : " + powerGenerated_hostel_solar);
                    System.out.println("Hostel Wind Generation : " + powerGenerated_hostel_wind);

                    System.out.println("Department Load : " + powerDemand_department_Str);
                    System.out.println("Department Solar + wind Generation : " + powerGenerated_department);
                    System.out.println("Department Solar Generation : " + powerGenerated_department_solar);
                    System.out.println("Department Wind Generation : " + powerGenerated_department_wind);

                    //It's compared the power generated of each generated with the power demanded
                    powerGenerada = 0;
                    i = 0;
                    numOrderDemand = 0;//Variable that show how many Demand's Order must be sent to satisfy demand
                    powerDemand = Float.valueOf(powerDemand_Str);

                    numOrderDemand=AgentsGenerators.length;

                    System.out.println("Total no of Generators: "+ numOrderDemand);
                    for(int p=0;p<numOrderDemand;p++) {
                        //ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        bestSeller = AgentsGenerators[p];

                        System.out.println("Control Manager sends supply order to receiver" + bestSeller);

                        if (AgentsGenerators[p].toString().contains("Hostel")) {
                            if(AgentsGenerators[p].toString().contains("Solar"))
                            {
                            ACLMessage order1 = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            order1.addReceiver(bestSeller);
                            order1.setContent(powerDemand_hostel_Str);
                            order1.setConversationId("H_PM");
                            order1.setReplyWith("Order1" + System.currentTimeMillis());
                            myAgent.send(order1);
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("H_PM"),
                                    MessageTemplate.MatchInReplyTo(order1.getReplyWith()));
                            }
                            else{
                                ACLMessage order1 = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            order1.addReceiver(bestSeller);
                            order1.setContent(powerDemand_hostel_Str);
                            order1.setConversationId("H_WPM");
                            order1.setReplyWith("Order1" + System.currentTimeMillis());
                            myAgent.send(order1);
                            mt1 = MessageTemplate.and(MessageTemplate.MatchConversationId("H_WPM"),
                                    MessageTemplate.MatchInReplyTo(order1.getReplyWith()));
                            }
                        } else {
                            if(AgentsGenerators[p].toString().contains("Solar"))
                            {
                            ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            order.addReceiver(bestSeller);
                            order.setContent(powerDemand_department_Str);
                            order.setConversationId("D_PM");
                            order.setReplyWith("Order" + System.currentTimeMillis());
                            myAgent.send(order);
                            mt2 = MessageTemplate.and(MessageTemplate.MatchConversationId("D_PM"),
                                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                            }
                            else{
                                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            order.addReceiver(bestSeller);
                            order.setContent(powerDemand_department_Str);
                            order.setConversationId("D_WPM");
                            order.setReplyWith("Order" + System.currentTimeMillis());
                            myAgent.send(order);
                            mt3 = MessageTemplate.and(MessageTemplate.MatchConversationId("D_WPM"),
                                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                            }
                        }



                        //System.out.println("Power Manager sends a supply order for a power of: "+order.getContent()+"al receptor "+bestSeller);
                        // Prepare the template to get the purchase order reply
                    }
                    step = 3;
                    break;
                case 3:

                    // Receive the purchase order reply
                    myAgent.doWait(5000);

                    
                    ACLMessage reply_mt = myAgent.receive(mt);
                    ACLMessage  reply_mt1 =myAgent.receive(mt1);
                    ACLMessage  reply_mt2 =myAgent.receive(mt2);
                    ACLMessage  reply_mt3 =myAgent.receive(mt3);

                    if(reply_mt1==null || reply_mt==null || reply_mt2 ==null||reply_mt3 ==null)
                    {
                        myAgent.doWait(3000);
                        
                        reply_mt = myAgent.receive(mt);
                        reply_mt1 =myAgent.receive(mt1);
                        reply_mt2 =myAgent.receive(mt2);
                        reply_mt3 =myAgent.receive(mt3);

                    }

                    if (reply_mt != null) {
                        // Purchase order reply received
                        if (reply_mt.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(reply_mt.getContent()+"(kW)"+" successfully supplied from agent "+reply_mt.getSender().getName()+" "+ myAgent.getName());
                            //mt=mt1;
                        }
                        else {
                            System.out.println("fault: the requested power is already supplied.");

                        }
                    }
                       // reply_mt1 = myAgent.receive(mt1);

                        if (reply_mt1 != null) {
                            // Purchase order reply received
                            if (reply_mt1.getPerformative() == ACLMessage.INFORM) {
                                // Purchase successful. We can terminate
                                System.out.println(reply_mt1.getContent()+"(kW)"+" successfully supplied from agent "+reply_mt1.getSender().getName()+" a "+ myAgent.getName());
                                // mt=mt1;
                                step=4;
                                repliesCnt++;
                            }
                            else {
                                System.out.println("fault: the requested power is already supplied.");
                            }

                        }
                        if (reply_mt2 != null) {
                            // Purchase order reply received
                            if (reply_mt2.getPerformative() == ACLMessage.INFORM) {
                                // Purchase successful. We can terminate
                                System.out.println(reply_mt2.getContent()+"(kW)"+" successfully supplied from agent "+reply_mt2.getSender().getName()+" a "+ myAgent.getName());
                                // mt=mt1;
                                step=4;
                                repliesCnt++;
                            }
                            else {
                                System.out.println("fault: the requested power is already supplied.");
                            }

                        }
                        if (reply_mt3 != null) {
                            // Purchase order reply received
                            if (reply_mt3.getPerformative() == ACLMessage.INFORM) {
                                // Purchase successful. We can terminate
                                System.out.println(reply_mt3.getContent()+"(kW)"+" successfully supplied from agent "+reply_mt3.getSender().getName()+" a "+ myAgent.getName());
                                // mt=mt1;
                                step=4;
                                repliesCnt++;
                            }
                            else {
                                System.out.println("fault: the requested power is already supplied.");
                            }

                        }
                        else{
                            System.out.println("Can't find 2nd reply");
                            step=4;
                            repliesCnt++;
                        }

                    repliesCnt++;
               /*If the number of responses is equal to or greater than the number of registered generators
                means that you have received all proposals from all generators
                 and you can move on to the next stage.*/
                    //if (repliesCnt >= AgentsGenerators.length) {
                        // We received all replies
                        step = 4;
                        repliesCnt = 0;

                         hostelCredit_str="";
                         hostelDebit_str="";
                         departmentDebit_str="";
                         departmentCredit_str="";

                         hostelCredit=0;
                         hostelDebit=0;
                         departmentDebit=0;
                         departmentCredit=0;
                         final_check =0;

                        //verify the total availability after supply is completed
                        if (powerGenerated_hostel - powerDemand_hostel >= 0) {
                            hostelCredit = powerGenerated_hostel - powerDemand_hostel;
                            System.out.println("Hostel is in Credit of : " + hostelCredit);
                            hostelCredit_str = "hostelCredit:" + hostelCredit;
                        } else {
                            hostelDebit = powerGenerated_hostel - powerDemand_hostel;
                            System.out.println("Hostel is in Debit of : " + hostelDebit);
                            hostelDebit_str = "hostelDebit:" + hostelDebit;
                        }
                        if (powerGenerated_department - powerDemand_department >= 0) {
                            departmentCredit = powerGenerated_department - powerDemand_department;
                            System.out.println("Department is in Credit of : " + departmentCredit);
                            departmentCredit_str = "departmentCredit:" + departmentCredit;
                        } else {
                            departmentDebit = powerGenerated_department - powerDemand_department;
                            System.out.println("Department is in Debit of : " + departmentDebit);
                            departmentDebit_str = "departmentDebit:" + departmentDebit;

                        }

                        System.out.println(hostelCredit_str + "," + hostelDebit_str + "," + departmentCredit_str + "," + departmentDebit_str);

                        String nums[] = new String[]{hostelCredit_str, hostelDebit_str, departmentCredit_str, departmentDebit_str};

                        for (int k = 0; k < nums.length; k++) {
                            if (!nums[k].isEmpty()) {
                            float value = Float.parseFloat(nums[k].split(":")[1]);
                            
                            final_check=value+final_check;
                            }
                            System.out.println("final_check = "+final_check);

                        }
                        for (int j = 0; j < nums.length; j++) {

                            if (!nums[j].isEmpty()) {
                                float value = Float.parseFloat(nums[j].split(":")[1]);
                                String name = nums[j].split(":")[0];
                                if (value > 0 || value < 0) {
                                    System.out.println(name + ":" + value);

                                    if (name.contains("Credit")) {
                                        if (name.contains("hostel")) {

                                            System.out.println("Hostel is in Credit, So look if department has met load requirement");
                                            if (departmentDebit < 0) {
                                                System.out.println("Department is in Debit, so hostel Credit will go department load first before hostel battery");
                                                 final_check = hostelCredit + departmentDebit;


                                                if (final_check > 0) {
                                                    System.out.println("Department debit :" + departmentDebit + " has been successfully supplied from hostel surpluss");
                                                    System.out.println("Surpluss hostel generation will go into battery with less SOC in the grid");

                                                } else if (final_check < 0) {
                                                    System.out.println(hostelCredit + "kw Of Department debit :" + departmentDebit + " is  successfully supplied from hostel surpluss and " + final_check + "is yet to be supplied and this will be taken from Department battery");

                                                }
                                            } else {
                                                System.out.println("Department is also in credit, so lets look into Battery SOC for both hostel and Department");
                                            }
                                        } else {
                                            System.out.println("Department is in Credit look of Hostel has met load rquirement");
                                            if (hostelDebit < 0) {
                                                System.out.println("Hostel is in Debit, so Department Credit will go Hostel load first before Department battery");
                                                 final_check = departmentCredit + hostelDebit;


                                                if (final_check > 0) {
                                                    System.out.println("Hostel debit :" + hostelDebit + " has been successfully supplied from Department surpluss");
                                                    System.out.println("Surpluss Department generation will go into battery with less SOC in the grid");

                                                } else if (final_check < 0) {
                                                    System.out.println(departmentCredit + "kw Of Hostel debit :" + hostelDebit + " is  successfully supplied from Department surpluss and " + final_check + "is yet to be supplied and this will be taken from Hostel battery");

                                                }

                                            }
                                            else
                                            { System.out.println("Hostel is also in credit, so lets look into Battery SOC for both hostel and Department");
                                                final_check=hostelCredit+departmentCredit;

                                            }

                                        }
                                    } else {
                                        if (name.contains("hostel")) {
                                            System.out.println("Hostel is in Debit, So look if department has surpluss generation");
                                            if (departmentCredit > 0) {
                                                System.out.println("Department Generation is in surpluss, so Department Credit will go Hostel load first before Department battery");
                                                 final_check = hostelDebit + departmentCredit;


                                                if (final_check > 0) {
                                                    System.out.println("Hostel debit :" + hostelDebit + " has been successfully supplied from Department surpluss");
                                                    System.out.println("Surpluss Department generation will go into battery with less SOC in the grid");

                                                } else if (final_check < 0) {
                                                    System.out.println(departmentCredit + "kw Of Hostel debit :" + hostelDebit + " is  successfully supplied from Department surpluss and " + final_check + "is yet to be supplied and this will be taken from Hostel battery");

                                                }
                                            } else if(departmentDebit<0) {
                                                System.out.println("Department is also in debit, so lets look into Battery SOC for both hostel and Department");
                                            }
                                        } else {

                                            System.out.println("Department is in Debit, So look if hostel has surpluss generation");
                                            if (hostelCredit > 0) {
                                                System.out.println("hostel Generation is in surpluss, so hostel Credit will go Department load first before hostel battery");
                                                 final_check = hostelCredit + departmentDebit;

                                                if (final_check > 0) {
                                                    System.out.println("Department debit :" + departmentDebit + " has been successfully supplied from Hostel surpluss");
                                                    System.out.println("Surpluss hostel generation will go into battery with less SOC in the grid");

                                                } else if (final_check < 0) {
                                                    System.out.println(hostelCredit + "kw Of Department debit :" + departmentDebit + " is  successfully supplied from hostel surpluss and " + final_check + "is yet to be supplied and this will be taken from Department battery");

                                                }
                                            } else if(hostelDebit<0) {
                                                System.out.println("Hostel is also in debit, so lets look into Battery SOC for both hostel and Department");
                                                final_check = hostelDebit + departmentDebit;

                                            }
                                        }
                                    }
                                }else {
                                    continue;
                                }
                            } else {
                                continue;
                            }
                        }
                        break;

                    //}else {
                       // block();
                    //}
                   // break;
                case 4:
                    // Verify the hostel and department credit and Debit and get battery ready for it.
                    myAgent.doWait(3000);

//                    float battery_hostel_soc;
//                    float battery_department_soc;

                    if(final_check!=0)
                    {
                        DFAgentDescription template_batt = new DFAgentDescription();
                        ServiceDescription sd_batt = new ServiceDescription();
                        sd_batt.setType("pcc-baterias");
                        template_batt.addServices(sd_batt);

                        try {
                            DFAgentDescription[] result_batt = DFService.search(myAgent, template_batt);
                            dfbatts=result_batt;
                            if(result_batt.length>0){
                                System.out.println("The following battery systems are found:");
                                AgentsBatteries = new AID[result_batt.length];

                                for (i = 0; i < result_batt.length; ++i) {
                                    AgentsBatteries[i] = result_batt[i].getName();
                                    System.out.println(AgentsBatteries[i].getName());
                                }
                            }else{
                                System.out.println("Waiting for batteries ...");
                                block();
                                myAgent.doWait(20000);
                                break;

                            }
                        }catch(FIPAException fe){
                            fe.printStackTrace();
                        }

                        StrategyControl  controlP=new StrategyControl();
                        float [] batt_input_=new float[3];

                        batt_input_=controlP.PeakShaving(final_check);
                        p_diff=Float.toString(batt_input_[0]);
                        status=Float.toString(batt_input_[1]);
                        threshold=Float.toString(batt_input_[2]);

                        batt_input="["+p_diff+","+status+","+threshold+"]";


                        ACLMessage cfp_batt = new ACLMessage(ACLMessage.CFP);
                        for (i = 0; i < AgentsBatteries.length; ++i) {
                            cfp_batt.addReceiver(AgentsBatteries[i]);
                        }
                        //p_generations=new int[AgentsGenerators.length];
                        battery_id=new AID[AgentsBatteries.length];
                        cfp_batt.setContent(batt_input);
                        cfp_batt.setConversationId("PM_BATT");
                        cfp_batt.setReplyWith("cfp_batt"+System.currentTimeMillis()); // Unique value
                        System.out.println("Control Manager sends CFP to the batteries with the message: "+cfp_batt.getContent());
                        myAgent.send(cfp_batt);


                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("PM_BATT"),
                                MessageTemplate.MatchInReplyTo(cfp_batt.getReplyWith()));
                        step=5;

                    }
                    else
                    {
                        System.out.println("\nThe demand has been covered with the generation\n.\n");
                        System.out.println("Battery action is not necessary.\n");
                        step=9;//Goes to the confirmation of the satisfaction of the demand
                        break;
                    }

                    break;

                case 5:
                         ACLMessage reply_batt = myAgent.receive(mt);
                         System.out.println("Control Manager tries to receive battery message. ");

                         n_batt=AgentsBatteries.length;

                    //If the battery information message is received
                    if (reply_batt != null) {
                        System.out.println("Power Manager receives a proposal from: "+reply_batt.getContent()+" Of battery: "+reply_batt.getSender());
                        // Reply received
                        if (reply_batt.getPerformative() == ACLMessage.PROPOSE) {

                            //Treatment of the proposal received
                            batt_propose = reply_batt.getContent();
                            String[] items_batt = batt_propose.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
                            float[] results_batt = new float[items_batt.length];

                            for (i = 0; i < items_batt.length; i++) {
                                try {
                                    results_batt[i]=Float.parseFloat(items_batt[i]);
                                }catch (NumberFormatException nfe) {
                                    //NOTE: write something here if you need to recover from formatting errors
                                }
                            }

                            p_batt=results_batt[0];
                            soc_batt=results_batt[1];
                            aCD_=results_batt[2];

                            if(aCD_==1){ aCD=1;}
                            if(aCD_==-1){ aCD=-1;}
                            if(aCD_==0){ aCD=0;}
                            System.out.println("Final_check: " +final_check +"(kW)");

                            //The name of the battery from which it has received power is detected
                            batt=reply_batt.getSender();
                            i=0;
                            if(k<n_batt){
                                agentsB1=powerSelect.DataStorage_batt(n_batt, batt, p_batt,soc_batt ,aCD);
                                k++;
                            }

                            if(k==n_batt){
                                //All battery values ​​have been stored
                                if(final_check>0){
                                    //When there is greater demand than generation
                                    agentsB2=powerSelect.DataOrganizer_batt_Descending(n_batt, agentsB1);
                                    bestOption_batt=agentsB2[0].GetArrayAgent_AID();
                                }
                                if(final_check<0){
                                    //When there is a greater generation that demands
                                    agentsB2=powerSelect.DataOrganizer_batt_Ascending(n_batt, agentsB1);
                                    bestOption_batt=agentsB2[0].GetArrayAgent_AID();
                                }
                            }
                        }
                        if(k<n_batt){
                            step=5;
                            break;
                        }else{
                            step=6;
                        }

                    }else{
                        step=5;
                    }

                    break;

                case 6:

                    //The total power resulting from the battery operations is calculated
                    //According to the pcc_inicial
                    n_batt=AgentsBatteries.length;

                    StrategyControl controlP_batts=new StrategyControl();
                    for(i=0;i<n_batt;++i){
                        System.out.println("agentB2: "+agentsB2[i].GetArrayAgent_AID());
                    }
//                    for(i=0;i<n_batt;++i){
//                        System.out.println("agentB2: "+agentsB2[i].GetArrayAgent_AID());
//                    }
                    powerBaterias=0;
                    i=0;
                    numOrderBatt=0;

                    if(final_check<0){
                        //To be within the -100 and 100 margins, the formula must be pcc_initial + battery power <p_upper
                        while(final_check+powerBaterias<controlP_batts.pcc_lower && final_check+powerBaterias<0 ){
                            aCD=agentsB2[i].GetArrayAgent_aCD();
                            if(aCD==0||aCD==1){
                                pBat=agentsB2[i].GetArrayAgent_p();
                                if(pBat<0){pBat=-pBat;}
                                powerBaterias=powerBaterias+pBat;
                                i++;
                                numOrderBatt++;
                            }else{
                                pBat=agentsB2[i].GetArrayAgent_p();
                                if(pBat<0){pBat=-pBat;}
                                powerBaterias=powerBaterias-pBat;
                                i++;
                                numOrderBatt++;
                            }
                            if(i==n_batt){
                                break;
                            }
                        }
                        //this.potenciaBaterias=potenciaBaterias;
                        this.numOrderBatt=numOrderBatt;
                    }

                    if(final_check>0){
                        //To be within the -100 and 100 margins, the formula must be pcc_initial + batterypower> p_lower
                        while(final_check-powerBaterias>controlP_batts.pcc_upper && final_check-powerBaterias>0 ){
                            aCD=agentsB2[i].GetArrayAgent_aCD();
                            if(aCD==0||aCD==-1){
                                pBat=agentsB2[i].GetArrayAgent_p();
                                if(pBat<0){pBat=-pBat;}
                                powerBaterias=powerBaterias+pBat;
                                i++;
                                numOrderBatt++;
                            }else{
                                pBat=agentsB2[i].GetArrayAgent_p();
                                if(pBat<0){pBat=-pBat;}
                                powerBaterias=powerBaterias-pBat;
                                i++;
                                numOrderBatt++;
                            }
                            if(i==n_batt){
                                break;
                            }
                        }
                        //this.powerBatteries=powerBatteries;
                        this.numOrderBatt=numOrderBatt;
                    }

                            /*If pcc_initial is within both limits, it is sent to all batteries 0 (kW) for
                            if they had to load or unload automatically*/
                    if(controlP_batts.pcc_lower<=final_check && final_check<=controlP_batts.pcc_upper){
                        //this.powerBatteries receives the value in the previous sentences
                        numOrderBatt=AgentsBatteries.length;
                        this.numOrderBatt=numOrderBatt;

                    }
                            /*In this case, the initialization and instance of object order_batt it's done here
                            because if it would be done before in any point this info is lost*/
                    ACLMessage order_batt= new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    for(i=0;i<numOrderBatt;i++){
                        bestOption_batt=agentsB2[i].GetArrayAgent_AID();
                        order_batt.addReceiver(bestOption_batt);
                        System.out.println("Power Manager gives order of action to the battery"+bestOption_batt);
                    }
                    if(final_check>0){System.out.println("THE TOTAL POWER THAT THE BATTERIES SHOULD CONSUME WOULD BE: " +powerBaterias+"(kW). \n");}
                    if(final_check<0){System.out.println("THE TOTAL POWER THAT THE BATTERIES SHOULD SUPPLY WOULD BE: " +powerBaterias+"(kW). \n");}
                    pcc_initial=Float.toString(final_check);
                    batt_input="["+p_diff+","+status+","+threshold+"]";

                    order_batt.setContent( batt_input);
                    order_batt.setConversationId("PM_BAT_Action");
                    order_batt.setReplyWith("Orden"+System.currentTimeMillis());
                    System.out.println("PM sends to the batteries: "+batt_input);
                    //******ERRRORRRR!!!!!!!!!!!!!!**********
                    myAgent.send(order_batt);

                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("PM_BAT_Action"),
                            MessageTemplate.MatchInReplyTo(order_batt.getReplyWith()));

                    step=7;
                    break;

                case 7:
                    float pcc_=0;
                    float soc_pre=0;
                    String soc_pre_str=new String();

                    //PRUEBA UNITARIA
                    if(powerDemand==1751){
                        powerDemand=1751;
                    }
                    if(powerDemand==1350){
                        powerDemand=1350;
                    }



                    pcc_initial_=powerDemand-this.powerGenerated;

                    reply_batt=myAgent.receive(mt);

                    if(reply_batt!=null){

                        if(reply_batt.getPerformative()==ACLMessage.INFORM){

                            numreplybatt++;
                            //Tratamiento de la propuesta recibida
                            batt_output = reply_batt.getContent();
                            String[] items_batt = batt_output.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
                            float[] results_batt = new float[items_batt.length];

                            for (i = 0; i < items_batt.length; i++) {
                                try {
                                    results_batt[i]=Float.parseFloat(items_batt[i]);
                                }catch (NumberFormatException nfe) {
                                    //NOTE: write something here if you need to recover from formatting errors
                                }
                            }
                            p_batt=results_batt[0];
                            soc_batt=results_batt[1];
                            aCD_=results_batt[2];
                            this._aCD=aCD_;
                            controlP_batts=new StrategyControl();
                            //BatteryAction batt_decision=new BatteryAction();
                            PowerSelector batt_decision=new PowerSelector();

                            //Conditions where the battery delivers energy
                            if(pcc_initial_<0 && pcc_initial_>=controlP_batts.pcc_lower && aCD_==-1  || pcc_initial_>0 && pcc_initial_>controlP_batts.pcc_upper && aCD_==0 || pcc_initial_>0 && pcc_initial_<=controlP_batts.pcc_upper && aCD_==-1){
                                //The value is subtracted because the battery is contributing
                                this.pBatts=this.pBatts-p_batt;
                            }
                            //Conditions where the battery absorbs energy
                            if(pcc_initial_<0 && pcc_initial_<controlP_batts.pcc_lower && aCD_==0 || pcc_initial_<0 && pcc_initial_>=controlP_batts.pcc_lower && aCD_==1|| pcc_initial_>0 && pcc_initial_<=controlP_batts.pcc_upper && aCD_==1){
                                //The value is added because the battery is absorbing
                                this.pBatts=this.pBatts+p_batt;
                            }
                            pcc_= pcc_initial_+this.pBatts;

                            if(pcc_initial_<0){

                                //It is checked that it does not conflict with the pcc limits.

                                if(numreplybatt<=this.numOrderBatt){
                                               /*If the last battery response has not been received,
                                               you have to worry for pcc_initial <0 that it does not exceed the upper limit,
                                               saving in arrays the data of the batteries. In such a way that when there is a
                                               battery that consumes a consumption that endangers the upper limit must
                                               that consumption of that battery be paralyzed, eliminating the steps and reestablishing
                                               the previous soc from that battery to automatic charging.*/

                                               /*To store values ​​globally, it must be necessary with an independent class
                                               this AgentFeatures class, can only receive elements of the same class, through
                                               other class powerSelector*/

                                    agentBatts=battData.DataStorage_batt(this.numOrderBatt, reply_batt.getSender(), p_batt, soc_batt, 0);

                                    if(pcc_>controlP_batts.pcc_upper){

                                        //Conflict for upper limit
                                        bestOption_batt=agentsB2[numreplybatt-1].GetArrayAgent_AID();
                                        //The type of delivery stop message is defined
                                        ACLMessage stop_batt= new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                        stop_batt.setConversationId("STOP_BATT");
                                        stop_batt.setReplyWith("Orden"+System.currentTimeMillis());
                                        stop_batt.addReceiver(bestOption_batt);
                                        soc_pre=agentsB2[numreplybatt-1].GetArrayAgent_soc();
                                        soc_pre_str=Float.toString(soc_pre);
                                        stop_batt.setContent( soc_pre_str);
                                        System.out.println("PM send to "+agentBatts[numreplybatt-1].GetArrayAgent_AID()+" action freeze to avoid conflict with PeakShaving upper limit:"+controlP_batts.pcc_upper);

                                        myAgent.send(stop_batt);

                                        //myAgent.doWait(1000);

                                    }else{

                                        //Si no entra en conflicto
                                        if(aCD_==1){
                                            System.out.println(p_batt+"(kW) "+"consume "+reply_batt.getSender().getName()+" through automatic loading phase.\n");
                                            System.out.println(soc_batt+"(%) "+"remains "+reply_batt.getSender().getName()+".\n");
                                            this.powerBattery =this.powerBattery +p_batt;
                                        }
                                        if(aCD_==-1){
                                            System.out.println(p_batt+"(kW) "+"contributes "+reply_batt.getSender().getName()+" \n" +
                                                    "through automatic discharge phase.\n");
                                            System.out.println(soc_batt+"(%) "+"remains "+reply_batt.getSender().getName()+".\n");
                                            this.powerBattery =this.powerBattery -p_batt;
                                        }
                                        if(aCD_==0){
                                            System.out.println(p_batt+"(kW) "+"consume "+reply_batt.getSender().getName());
                                            System.out.println(soc_batt+"(%) "+"remains "+reply_batt.getSender().getName()+".\n");

                                            if(p_batt==0){
                                                this.powerBattery =this.powerBattery +p_batt;
                                            }else{
                                                this.powerBattery =this.powerBattery +p_batt;
                                            }
                                        }
                                    }
                                }

                                if(numreplybatt==this.numOrderBatt){
                                               /*If the response of the last battery has been received,
                                               you have to worry for pcc_initial <0 that it does not exceed the lower limit,
                                               If it is exceeded and pcc_ <controlP_batts.pcc_lower then it would be necessary to eliminate
                                               of the sum pcc_ the powers of the batteries starting with the last until
                                               if pcc _> = controlP_batts.pcc_lower. Thus paralyzing the download process
                                               automatic removal of those batteries removed and reestablishing the soc prior to said discharge.*/
                                    i=1;
                                    if(pcc_<controlP_batts.pcc_lower && aCD_!=0){
                                        //Conflict by lower limit
                                        while(pcc_<controlP_batts.pcc_lower){

                                            float p_agentbatt=agentBatts[numreplybatt-i].GetArrayAgent_p();

                                                            /*p_agentbatt=agentBatts[1].GetArrayAgent_p();
                                                            p_agentbatt=agentBatts[2].GetArrayAgent_p();
                                                            p_agentbatt=agentBatts[3].GetArrayAgent_p();*/

                                            //Se define el tipo de mensaje de paralización de entrega
                                            ACLMessage stop_batt= new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                            stop_batt.setConversationId("STOP_BATT");
                                            stop_batt.setReplyWith("Orden"+System.currentTimeMillis());
                                            bestOption_batt=agentsB2[numreplybatt-i].GetArrayAgent_AID();
                                            stop_batt.addReceiver(bestOption_batt);
                                            soc_pre=agentsB2[numreplybatt-i].GetArrayAgent_soc();
                                            soc_pre_str=Float.toString(soc_pre);
                                            stop_batt.setContent( soc_pre_str);
                                            System.out.println("PM sends "+bestOption_batt+" action freeze to avoid conflict with lower limit of PeakShaving:"+controlP_batts.pcc_lower);

                                            myAgent.send(stop_batt);

                                            pcc_=pcc_+p_agentbatt;//It is added since pcc_ is negative and that difference must be reduced
                                            this.powerBattery =this.powerBattery +p_agentbatt;

                                            i++;
                                        }
                                        DecimalFormat p_baterias_df = new DecimalFormat("0.00");
                                        p_battery_Str =p_baterias_df.format(this.powerBattery);

                                        //****************SE ALMACE ARRAY P_BATT_TOTAL**************************
                                        //a_p_batt_total[a]=this.potenciaBaterias;
                                        a_p_batt_total[a]= p_battery_Str;
                                        //************************************************************************

                                    }else{

                                        this.powerBattery =batt_decision.BatteryDecision(aCD_, reply_batt.getSender().getName(), p_batt, soc_batt, this.powerBattery);


                                        DecimalFormat p_baterias_df = new DecimalFormat("0.00");
                                        p_battery_Str =p_baterias_df.format(this.powerBattery);

                                        //****************SE ALMACE ARRAY P_BATT_TOTAL*****************************
                                        //a_p_batt_total[a]=this.potenciaBaterias;
                                        a_p_batt_total[a]= p_battery_Str;
                                        //*************************************************************************

                                    }
                                }


                            }

                            if(pcc_initial_>0){

                                if(numreplybatt<=this.numOrderBatt){

                                    agentBatts=battData.DataStorage_batt(this.numOrderBatt, reply_batt.getSender(), p_batt, soc_batt, 0);

                                    if(pcc_<controlP_batts.pcc_lower){

                                        //Conflict by lower limit

                                        //The type of delivery stop message is defined
                                        ACLMessage stop_batt= new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                        stop_batt.setConversationId("STOP_BATT");
                                        stop_batt.setReplyWith("Orden"+System.currentTimeMillis());
                                        bestOption_batt=agentsB2[numreplybatt-1].GetArrayAgent_AID();
                                        stop_batt.addReceiver(bestOption_batt);
                                        soc_pre=agentsB2[numreplybatt-1].GetArrayAgent_soc();
                                        soc_pre_str=Float.toString(soc_pre);
                                        stop_batt.setContent( soc_pre_str);
                                        System.out.println("PM sends to"+agentBatts[numreplybatt-1].GetArrayAgent_AID()+" action freeze to avoid conflict with lower limit of PeakShaving:"+controlP_batts.pcc_lower);

                                        myAgent.send(stop_batt);


                                        //myAgent.doWait(1000);

                                    }else{

                                        //Si no entra en conflicto
                                        if(aCD_==1){
                                            System.out.println(p_batt+"(kW) "+"consume "+reply_batt.getSender().getName()+" through automatic loading phase.\n");
                                            System.out.println(soc_batt+"(%) "+"remains "+reply_batt.getSender().getName()+".\n");
                                            this.powerBattery =this.powerBattery +p_batt;
                                        }
                                        if(aCD_==-1){
                                            System.out.println(p_batt+"(kW) "+"contributes "+reply_batt.getSender().getName()+" via automatic discharge phase.\n");
                                            System.out.println(soc_batt+"(%) "+"remains "+reply_batt.getSender().getName()+".\n");
                                            this.powerBattery =this.powerBattery -p_batt;
                                        }
                                        if(aCD_==0){
                                            System.out.println(p_batt+"(kW) "+"contributes "+reply_batt.getSender().getName());
                                            System.out.println(soc_batt+"(%) "+"remains "+reply_batt.getSender().getName()+".\n");

                                            if(p_batt==0){
                                                this.powerBattery =this.powerBattery -p_batt;
                                            }else{
                                                this.powerBattery =this.powerBattery -p_batt;
                                            }
                                        }
                                    }
                                }
                                if(numreplybatt==this.numOrderBatt){
                                               /*If the response of the last battery has been received,
                                               you have to worry for pcc_initial <0 that it does not exceed the lower limit,
                                               If it is exceeded and pcc_ <controlP_batts.pcc_lower then it would be necessary to eliminate
                                               of the sum pcc_ the powers of the batteries starting with the last until
                                               if pcc _> = controlP_batts.pcc_lower. Thus paralyzing the download process
                                               automatic removal of those batteries removed and reestablishing the soc prior to said discharge.*/
                                    i=1;
                                    if(pcc_>controlP_batts.pcc_upper && aCD_!=0 ){
                                        //
                                        //Conflict by lower limit
                                        //while(pcc_>controlP_batts.pcc_upper || (numreplybatt-i)>=0){
                                        while(pcc_>controlP_batts.pcc_upper && (numreplybatt-i)>=0){
                                            if((numreplybatt-i)<0){
                                                break;
                                            }
                                            float p_agentbatt=agentBatts[numreplybatt-i].GetArrayAgent_p();

                                                            /*p_agentbatt=agentBatts[1].GetArrayAgent_p();
                                                            p_agentbatt=agentBatts[2].GetArrayAgent_p();
                                                            p_agentbatt=agentBatts[3].GetArrayAgent_p();*/

                                            //The type of delivery stop message is defined
                                            ACLMessage stop_batt= new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                            stop_batt.setConversationId("STOP_BATT");
                                            stop_batt.setReplyWith("Orden"+System.currentTimeMillis());
                                            bestOption_batt=agentsB2[numreplybatt-i].GetArrayAgent_AID();
                                            stop_batt.addReceiver(bestOption_batt);
                                            soc_pre=agentsB2[numreplybatt-i].GetArrayAgent_soc();
                                            soc_pre_str=Float.toString(soc_pre);
                                            stop_batt.setContent( soc_pre_str);
                                            System.out.println(agentBatts[numreplybatt-i].GetArrayAgent_AID()+" stops to avoid conflict with PeakShaving upper limit:"+controlP_batts.pcc_upper);

                                            myAgent.send(stop_batt);

                                            //float p_agentbatt=agentBatts[numreplybatt-i].GetArrayAgent_p();
                                            pcc_=pcc_-p_agentbatt;//It is subtracted since pcc_ is positive and that difference must be reduced
                                            //if(p_agentbatt>0){p_agentbatt=-p_agentbatt;}
                                            this.powerBattery =this.powerBattery -p_agentbatt;

                                            i++;
                                        }


                                        DecimalFormat p_baterias_df = new DecimalFormat("0.00");
                                        p_battery_Str =p_baterias_df.format(this.powerBattery);

                                        //****************IS STORED ARRAY P_BATT_TOTAL*****************************
                                        //a_p_batt_total[a]=this.potenciaBaterias;
                                        a_p_batt_total[a]= p_battery_Str;
                                        //*************************************************************************

                                    }else{

                                        this.powerBattery =batt_decision.BatteryDecision(aCD_, reply_batt.getSender().getName(), p_batt, soc_batt, this.powerBattery);


                                        DecimalFormat p_baterias_df = new DecimalFormat("0.00");
                                        p_battery_Str =p_baterias_df.format(this.powerBattery);

                                        //****************IS STORED ARRAY P_BATT_TOTAL*****************************
                                        //a_p_batt_total[a]=this.potenciaBaterias;
                                        a_p_batt_total[a]= p_battery_Str;
                                        //*************************************************************************

                                    }
                                }
                            }
                        }else{
                            if(reply_batt.getPerformative()==ACLMessage.REFUSE){
                                numreplybatt++;
                                System.out.println("Battery "+reply_batt.getSender().getName()+" stop, proper SOC\n.");
                            }
                        }

                        if(numreplybatt<this.numOrderBatt){
                            step= 7;
                            break;
                        }else{
                            step=8;
                        }
                    }else{
                        block();
                    }
                    break;

                case 8:
                    myAgent.doWait(1000);
                    controlP_batts=new StrategyControl();

                    if(a==26){
                        a=26;
                    }
                    if(a==27){
                        a=27;
                    }
                    if(a==28){
                        a=28;
                    }
                    pcc_initial_=powerDemand-this.powerGenerated;
                    if(pcc_initial_<0){

                        //if(this.potenciaBaterias<0){this.potenciaBaterias=-this.potenciaBaterias;}

                        if(pcc_initial_>=controlP_batts.pcc_lower ){
                            pcc_final_=pcc_initial_+this.powerBattery;
                            pcc_final=Float.toString(pcc_final_);

                            DecimalFormat pcc_final_df = new DecimalFormat("0.00");
                            pcc_final_Str=pcc_final_df.format(pcc_final_);

                            //****************ARRAY PCC_FINAL IS STORED*****************************
                            //a_pcc_final[a]=pcc_final_;
                            a_pcc_final[a]=pcc_final_Str;
                            //**********************************************************************

                        }

                        if(pcc_initial_<controlP_batts.pcc_lower ){
                            pcc_final_=pcc_initial_+this.powerBattery;
                            pcc_final=Float.toString(pcc_final_);

                            DecimalFormat pcc_final_df = new DecimalFormat("0.00");
                            pcc_final_Str=pcc_final_df.format(pcc_final_);

                            //****************ARRAY PCC_FINAL IS STOREDL*****************************
                            //a_pcc_final[a]=pcc_final_;
                            a_pcc_final[a]=pcc_final_Str;
                            //**********************************************************************

                        }

                    }
                    if(pcc_initial_>0){
                        //if(this.potenciaBaterias<0){this.potenciaBaterias=-this.potenciaBaterias;}

                        if(pcc_initial_<=controlP_batts.pcc_upper ){
                            pcc_final_=pcc_initial_+this.powerBattery;
                            pcc_final=Float.toString(pcc_final_);


                            DecimalFormat pcc_final_df = new DecimalFormat("0.00");
                            pcc_final_Str=pcc_final_df.format(pcc_final_);

                            //****************ARRAY PCC_FINAL IS STORED*****************************
                            //a_pcc_final[a]=pcc_final_;
                            a_pcc_final[a]=pcc_final_Str;
                            //**********************************************************************

                        }

                        if(pcc_initial_>controlP_batts.pcc_upper ){
                            pcc_final_=pcc_initial_+this.powerBattery;
                            pcc_final=Float.toString(pcc_final_);

                            DecimalFormat pcc_final_df = new DecimalFormat("0.00");
                            pcc_final_Str=pcc_final_df.format(pcc_final_);

                            //****************ARRAY PCC_FINAL IS STORED*****************************
                            //a_pcc_final[a]=pcc_final_;
                            a_pcc_final[a]=pcc_final_Str;
                            //**********************************************************************

                        }

                    }

                    System.out.println("Pcc_final: "+pcc_final+"(kW)");
                    step=9;
                    break;

                case 9:
                    //ACLMessage commit_demand=new ACLMessage(ACLMessage.CONFIRM);

//                    DecimalFormat p_baterias_df = new DecimalFormat("0.00");
//                    p_battery_Str =p_baterias_df.format(this.powerBattery);
//                    DecimalFormat pcc_final_df = new DecimalFormat("0.00");
//                    pcc_final_Str=pcc_final_df.format(Float.parseFloat(pcc_final));
//
//                    //****************ARRAY P_BATT_TOTA IS STORED**************************
//                    //a_p_batt_total[a]=this.potenciaBaterias;
//                    a_p_batt_total[a]= p_battery_Str;
//                    //**********************************************************************
//
//                    //****************ARRAY PCC_FINAL IS STORED*****************************
//                    //a_pcc_final[a]=pcc_final_;
//                    a_pcc_final[a]=pcc_final_Str;
//                    //**********************************************************************


                    //The consumer is wanted to send it to him

                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("LoadRequest-PM");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);

                        if(result.length>0){
                            AgentConsumers = new AID[result.length];
                            for (i = 0; i < result.length; ++i) {
                                AgentConsumers[i] = result[i].getName();
                            }
                        }else{
                            System.out.println( "No consumer could be contacted ...");
                            count++;
                            if (count>24){
                                myAgent.doDelete();
                            }

                        }

                    }catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Send the cfp to all sellers
                    for (i = 0; i < AgentConsumers.length; ++i) {
                        ACLMessage commit_demand=new ACLMessage(ACLMessage.CONFIRM);
                        //commit_demand.addReceiver(AgentConsumers[i]);
                        commit_demand.addReceiver(aidLoads[i]);
                        commit_demand.setContent(mLoads[i][1]);
                        //commit_demand.setContent("Demand point satisfied");
                        commit_demand.setConversationId("PM_C");
                        commit_demand.setReplyWith("commit_demand"+System.currentTimeMillis()); // Unique value
                        myAgent.send(commit_demand);
                    }
                    consumer_id=new AID[AgentConsumers.length];
                    mt=MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
                    step=10;
                    a++;
                    pDemand =0;
                    break;

                case 10:

                    addBehaviour(new FinishMessage());
                    ACLMessage finish=receive(mt);
                    if(finish!=null){
                        System.out.println();
                        myAgent.doDelete();
                    }


                    //Hour of the day, Hostel Load ,Hostel Solar, Hostel Battery,,Hostel Credit, HostelDebit, Department Load, Department Solar, Department Battery,Department Debit, Department Credit


                    System.out.println("In Step 10");
                    ExportCSV csv= new ExportCSV();

                    String [] HostelDetails={powerDemand_hostel_Str,powerGenerated_hostel+"",hostelCredit_str,hostelDebit_str};
                    String [] DepartmentDetails={powerDemand_department_Str,powerGenerated_department+"",departmentCredit_str,departmentDebit_str};

                    csv.CreateFinalCSVFile("/Users/sreeramvennapusa/Desktop/jade/AgentJadePHD/src/resource/finaloutput.csv",hourOfDay,pcc_final_Str,HostelDetails,DepartmentDetails);
                    hourOfDay++;
                    powerGenerated_department=0;
                    powerGenerated_hostel=0;
                    powerGenerated_hostel_solar=0;
                    powerGenerated_hostel_wind=0;
                    powerGenerated_department_solar=0;
                    powerGenerated_department_wind=0;
                    step= 11;
                    break;
            }

        }

        public boolean done() {

            return false;
//
//            if (step == 2 && bestSeller== null) {
//
//                System.out.println(
//                        "Failure: "+powerDemand_Str+" not available");
//            }
//            if(dfbatts.length<=0){
//
//                step=4;
//            }
//            return ((step == 2 && bestSeller == null) || step == 11);
        }
    }
    private class FinishMessage extends CyclicBehaviour{

        private MessageTemplate mt; // The template to receive replies

        public void action() {

             }
    }



}
