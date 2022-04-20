package org.MicroGridJade;


import jade.core.Agent;

public class MainContainerAgent  extends Agent {


    public static void main(String [] args)
    {
        String [] args1 ={"-gui"};


        jade.Boot.main(args1);

        String [] args2 ={"-container","BatteryHostel:org.MicroGridJade.Battery;SolarHostel:org.MicroGridJade.SolarGeneratorHostel;WindHostel:org.MicroGridJade.WindGeneratorHostel;LoadHostel:org.MicroGridJade.LoadHostel"};
        String [] args3 ={"-container","BatteryDepartment:org.MicroGridJade.Battery;SolarDepartment:org.MicroGridJade.SolarGeneratorDepartment;WindDepartment:org.MicroGridJade.WindGeneratorDepartment;LoadDepartment:org.MicroGridJade.LoadDepartment"};



        jade.Boot.main(args2);
        jade.Boot.main(args3);




    }


}
