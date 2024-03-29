/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.MicroGridJade;

/** @author endryys*/

import com.opencsv.CSVWriter;

import java.io.*;
import java.util.Date;

public class ExportCSV {


    public void CreateFinalCSVFile(String filepath,int hourOfDay,String pcc_final,String [] MicroGrid1Details,String [] MicroGrid2details,String [] Batterydetails, int count)
    {
        try {
            FileWriter output;
            if(count==1){
                output  = new FileWriter(filepath, false);
            }
             output = new FileWriter(filepath, true);
            CSVWriter write = new CSVWriter(output);

           if(hourOfDay==0) {

               // Header column value
               String[] header = {"Hour of the Day", "MicroGrid1 Load", "MicroGrid1 Generation","Solar MicroGrid1 Generation","Wind MicroGrid1 Generation", "MicroGrid1 Credit", "MicroGrid1 Debit", "H Battery SOC", "MicroGrid2 Load", "MicroGrid2 Generation","Solar MicroGrid2 Generation","Wind MicroGrid2 Generation", "MicroGrid2 Credit", "MicroGrid2 Debit","D battery SOC", "Battery Action"};
               write.writeNext(header);
           }
            // Value
            String[] data1 = {hourOfDay+"",MicroGrid1Details[0],MicroGrid1Details[1],MicroGrid1Details[2],MicroGrid1Details[3],MicroGrid1Details[4],MicroGrid1Details[5],Batterydetails[0],
            MicroGrid2details[0],MicroGrid2details[1],MicroGrid2details[2],MicroGrid2details[3],MicroGrid2details[4],MicroGrid2details[5],Batterydetails[1],Batterydetails[2]};
            write.writeNext(data1);
            write.close();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }
    }

    public void CreatefileCSV(String filepath, String delim,String[] pcc_initial, String[] pcc_final, String[] p_batt ){
        
        final String NEXT_LINE = "\n";
        int i=0;
        String pcc_initial_str,pcc_final_str,p_batt_str=new String();
        
        try {
		FileWriter fw = new FileWriter(filepath);
                
                /*fw.append("pcc_initial").append(NEXT_LINE);
                for(i=0; i<29;i++){
                  fw.append(Float.toString(pcc_initial[i])).append(NEXT_LINE);  
                }*/
                
                fw.append("pcc_initial;p_batt;pcc_final\n");
                for(i=0; i<47;i++){
                  
                    if(i==26){
                        i=26;
                    }
                  //pcc_initial_str=Float.toString(pcc_initial[i]);
                  pcc_initial_str=pcc_initial[i];
                  pcc_final_str=pcc_final[i];
                  p_batt_str=p_batt[i];
                  
                  fw.append(pcc_initial_str+";"+p_batt_str+";"+pcc_final_str+"\n"); 
                }
                
		/*fw.append("testing").append(delim);
		fw.append("123").append(NEXT_LINE);

		fw.append("value1");
		fw.append(delim);
		fw.append("312");
		fw.append(NEXT_LINE);

		fw.append("anotherthing,888\n");*/

		fw.flush();
		fw.close();
		} catch (IOException e) {
		// Error al crear el archivo, por ejemplo, el archivo 
		// está actualmente abierto.
		e.printStackTrace();
		}
    }
    
    //public void CreatefileCSVBATT(String filepath, String delim,float[] p_batt, float[] soc_batt){
    public void CreatefileCSVBATT(String filepath, String delim,String[] p_batt, String[] soc_batt){
        final String NEXT_LINE = "\n";
        int i=0;
        String p_batt_str,soc_batt_str=new String();
        
        try {
		FileWriter fw = new FileWriter(filepath);
                
                /*fw.append("pcc_initial").append(NEXT_LINE);
                for(i=0; i<29;i++){
                  fw.append(Float.toString(pcc_initial[i])).append(NEXT_LINE);  
                }*/
                
                fw.append("p_batt;soc\n");
                for(i=0; i<47;i++){
                  p_batt_str=p_batt[i];
                  soc_batt_str=soc_batt[i];
                  /*p_batt_str=Float.toString(p_batt[i]);
                  soc_batt_str=Float.toString(soc_batt[i]);*/
                  
                  fw.append(p_batt_str+";"+ soc_batt_str+"\n"); 
                }
                
		/*fw.append("testing").append(delim);
		fw.append("123").append(NEXT_LINE);

		fw.append("value1");
		fw.append(delim);
		fw.append("312");
		fw.append(NEXT_LINE);

		fw.append("anotherthing,888\n");*/

		fw.flush();
		fw.close();
		} catch (IOException e) {
		// Error al crear el archivo, por ejemplo, el archivo 
		// está actualmente abierto.
		e.printStackTrace();
		}
    }
     
            
  /* public static void main(String[] args) {
     
        List<usuario> usuarios = new ArrayList<usuario>();
         
        usuarios.add(new Usuario("1001","Jose","Ramirez Torres","jramirez89@hotmail.com"));
        usuarios.add(new Usuario("1002","Saul","Gaviria Garcia","sgaviria12@gmail.com"));
        usuarios.add(new Usuario("1003","Maria","Torres Mendoza","mtorres12@yahoo.com"));
         
        String outputFile = "test/usuarios_export.csv";
        boolean alreadyExists = new File(outputFile).exists();
         
        if(alreadyExists){
            File ficheroUsuarios = new File(outputFile);
            ficheroUsuarios.delete();
        }        
         
        try {
         
            CsvWriter csvOutput = new CsvWriter(new FileWriter(outputFile, true), ',');
             
            csvOutput.write("Codigo");
            csvOutput.write("Nombres");
            csvOutput.write("Apellidos");
            csvOutput.write("Correo");
            csvOutput.endRecord();
             
            for(Usuario us : usuarios){
                 
                csvOutput.write(us.getCodigo());
                csvOutput.write(us.getNombres());
                csvOutput.write(us.getApellidos());
                csvOutput.write(us.getCorreo());
                csvOutput.endRecord();                   
            }
             
            csvOutput.close();
         
        } catch (IOException e) {
            e.printStackTrace();
        }
    } */
}
