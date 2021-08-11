package org.MicroGridJade;


import jade.core.Agent;
import jade.core.AgentContainer;

public class MainContainerAgent  extends Agent {



    public static void main(String [] args)
    {
        String [] args1 ={"-gui"};


        jade.Boot.main(args1);

        String [] args2 ={"-container","BatteryHostel:org.MicroGridJade.Battery;SolarHostel:org.MicroGridJade.SolarGeneratorHostel;LoadHostel:org.MicroGridJade.LoadHostel"};
        String [] args3 ={"-container","BatteryDepartment:org.MicroGridJade.Battery;SolarDepartment:org.MicroGridJade.SolarGeneratorDepartment;LoadDepartment:org.MicroGridJade.LoadDepartment"};



        jade.Boot.main(args2);
        jade.Boot.main(args3);




    }


}
