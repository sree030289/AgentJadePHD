package org.MicroGridJade;


import jade.core.Agent;

public class MainContainerAgent  extends Agent {


    public static void main(String [] args)
    {
        String [] args1 ={"-gui"};


        jade.Boot.main(args1);

        String [] args2 ={"-container","BatteryMicroGrid1:org.MicroGridJade.Battery;SolarMicroGrid1:org.MicroGridJade.SolarGeneratorMicroGrid1;WindMicroGrid1:org.MicroGridJade.WindGeneratorMicroGrid1;LoadMicroGrid1:org.MicroGridJade.LoadMicroGrid1"};
        String [] args3 ={"-container","BatteryMicroGrid2:org.MicroGridJade.Battery;SolarMicroGrid2:org.MicroGridJade.SolarGeneratorMicroGrid2;WindMicroGrid2:org.MicroGridJade.WindGeneratorMicroGrid2;LoadMicroGrid2:org.MicroGridJade.LoadMicroGrid2"};



        jade.Boot.main(args2);
        jade.Boot.main(args3);




    }


}
