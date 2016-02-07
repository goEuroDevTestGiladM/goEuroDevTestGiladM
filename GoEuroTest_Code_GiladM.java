/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GoEuroTest_GiladM;

import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.file.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import GoEuroTest_GiladM.GoEuroTest_GiladM.Point.Geo_position;

/**
 * My solution to the GoEuro Java Developer Test.
 * For JSON parsing I am using Google's GSON library:
 * https://github.com/google/gson
 * 
 * @author GiladM
 */
public class GoEuroTest_GiladM {

    /**
     * A custom class defining the incoming JSON objects, I have named 'points'.
     * Since no explicit definition (only examples) of the Fields
     * of a point (JSON keys), as well as their exact value types was specified,
     * I am implementing only the Fields I actually need (_id, name, type, geo_position)
     * and treat all of them as strings (which will work regardless of the actual value types).
     */
    static class Point {
        public static class Geo_position {
            String latitude;
            String longitude;
        }

        String _id;
        String name;
        String type;
        Geo_position geo_position;
    }
    
    public static void main(String[] args) throws Exception {
        try {
            // Make sure a city_name argument was given
            if (args.length < 1) {
                errPrintAndExit("please supply a city name.");
            }
            String city_name = args[0];
            
            URL url = new URL("http://api.goeuro.com/api/v2/position/suggest/en/" + city_name);        
            Path CSVfilePath = Paths.get(city_name + ".csv");

            // Open URL & JSON stream readers
            System.out.println("Connecting to the GoEuro API");
            try (
                InputStreamReader URLin = new InputStreamReader(url.openStream(), "UTF-8");
                JsonReader JSONin = new JsonReader(URLin);
            ) {
                // Open CSV writer
                System.out.println("Creating the CSV file");
                // Delete the file if already exists
                Files.deleteIfExists(CSVfilePath);
                try (
                    BufferedWriter CSVout = Files.newBufferedWriter(CSVfilePath)
                ) {
                    System.out.println("Writing data to the CSV file");
                    // Write column headers
                    CSVout.write("_id,name,type,latitude,longitude");
                    CSVout.newLine();
                    
                    // Look for the JSON array beginning token
                    JSONin.beginArray();
                    
                    Gson gson = new Gson();
                    while (JSONin.hasNext()) {
                        // Gson will look for the next point (JSON object),
                        // comparing to the Point class structure
                        // and return it as an object (if exists)
                        Point point = gson.fromJson(JSONin, Point.class);

                        // Create a CSV line string from the point Fields
                        StringJoiner sj;
                        sj = new StringJoiner(",");
                        sj.add(point._id);
                        sj.add(point.name);
                        sj.add(point.type);
                        if (point.geo_position == null)
                            point.geo_position = new Geo_position();
                        sj.add(point.geo_position.latitude);
                        sj.add(point.geo_position.longitude);
                        String SCVline = sj.toString();

                        // Write to file
                        CSVout.write(SCVline);
                        CSVout.newLine();
                        CSVout.flush();
                    }
                    
                    // Look for the JSONarray end token
                    JSONin.endArray();
                }
                catch (Exception exc) {
                    
                    // Delete the CSV file (if exists)
                    Files.deleteIfExists(CSVfilePath);
                    
                    String excName = exc.getClass().getSimpleName();
                    // If the exception was thrown by one of the GSON methods
                    if (Arrays.asList(
                        "MalformedJsonException",
                        "EOFException",
                        "JsonSyntaxException",
                        "IllegalStateException"
                    ).contains(excName)) {
                        errPrintAndExit("the JSON data is corrupt or not in the excpected format.");
                    }
                    // If the exception was thrown by one of the CSV handling methods
                    else {
                        errPrintAndExit("unable to write to the CSV file. "
                                        + "Make sure the file is not write-protected, not currently in use "
                                        + "and that the program has file management permissions for the current path, "
                                        + "and try again.");
                    }
                }
            }
            catch (IOException | JsonIOException exc) {
                errPrintAndExit("unable to read data from the GoEuro API. Please try again.");
            }

            System.out.println("Done! the file " + CSVfilePath.toString() + " was created in the current path.");
            
        }
        catch (Exception exc) {
            errPrintAndExit("an unexcpected error has occured. Please try again.");
        }
    }
    
    /**
     * Print given text as an error to the System.err stream, and exit the program with exit code 1
     */
    static void errPrintAndExit(String description){
        System.err.println("Error - " + description);
        System.exit(1);
    }
}