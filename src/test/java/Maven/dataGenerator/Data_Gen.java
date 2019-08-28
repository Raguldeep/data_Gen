
package Maven.dataGenerator;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.codoid.products.exception.FilloException;
import com.codoid.products.fillo.Connection;
import com.codoid.products.fillo.Fillo;
import com.codoid.products.fillo.Recordset;

import jxl.write.WriteException;

/************************************************
 * @author - Ragul Deep
 * @Date - 20-Aug-2019
 * @ProjectName - Data-Generator
 * @tags - Data Generator,Runtime accelerator 
 * TODO - 
 ************************************************
 **/

public class Data_Gen {

	static String Value = "";
	static String config_Path = "C:\\Users\\daisymanik.MAVERICSYSTEMS\\git\\dataGen\\maven\\config\\Config_Parameter_file.properties";
	static String excelFileLocation = "D:\\Ragul\\Sheets\\MasterTestData.xlsx";
	static String result = "";
	static String resultss = "";
	static String FileOutputStream = "";

	static String[] splitColon;
	static String testcasename;

	public static void main(String[] args) throws IOException, FilloException, WriteException, InterruptedException {

		// Load the Configuration Property Files
		InputStream input = new FileInputStream(config_Path);
		Properties prop = new Properties();
		prop.load(input);
		prop.getProperty("NoofRecords");

		System.out.println(prop.getProperty("NoofRecords"));
		int noofRows = Integer.parseInt(prop.getProperty("NoofRecords"));

		ArrayList<String> alFields = new ArrayList<String>();
		ArrayList<String> recordData = new ArrayList<String>();
		ArrayList<String> recordScen = new ArrayList<String>();

		// Store the Configuration Data's into ArrayList
		ArrayList<String> result = new ArrayList<String>();
		for (Entry<Object, Object> entry : prop.entrySet()) {
			if (((String) entry.getKey()).contains("-")) {
				// result.add((String) entry.getValue());
				resultss = resultss + entry + "," + "";
			}

		}

		// Create Master Sheet, Input Scenario ID and Column Name
		if (prop.getProperty("CreateMasterSheet").contains("true")) {
			XSSFWorkbook workbook = new XSSFWorkbook();
			XSSFSheet sheet = workbook.createSheet("Sheet");
			XSSFRow row;

			System.out.println("NoOfRows =" + noofRows);
			sheet.createRow(0).createCell(0).setCellValue("Scenario_ID");

			for (int i = 1; i <= noofRows; i++) {
				row = sheet.createRow(i);
				row.createCell(0).setCellValue("SC_" + i);
			}
			String header = resultss;
			String[] array = header.split(",");

			System.out.println("NoOfColumns =" + array.length);
			for (int i = 0; i < array.length; i++) {
				row = sheet.getRow(0);
				row.createCell(i + 1).setCellValue(array[i].split("=")[0].split("-")[0]);
			}

			FileOutputStream file = new FileOutputStream(new File(excelFileLocation));
			workbook.write(file);

			HashMap<String, String> mastSheet = new HashMap<String, String>();
			HashMap<String, String> scenSheet = new HashMap<String, String>();
			HashMap<String, String> dataSheet = new HashMap<String, String>();
			HashMap<String, HashMap<String, String>> scenario_data = new HashMap<String, HashMap<String, String>>();
			
			//Initiating Fillo to read MasterSheet(xls) file Data and assign in to Hashmap
			Fillo fillo = new Fillo();

			Connection mast = fillo.getConnection(prop.getProperty("MasterSheetPath"));
			Connection data = fillo.getConnection(prop.getProperty("DataSourcePath"));
			Connection scen = fillo.getConnection(prop.getProperty("ScenarioSourcePath"));

			ReadData(array, mast, data, scen, alFields, recordData, dataSheet, scenSheet, scenario_data, recordScen);

			workbook.close();

			System.out.println("size OF DATA- " + dataSheet.size());
			System.out.println("size OF SCEN- " + scenSheet.size());
			
			//Updating senario data generator file header value from Mastersheet to Data Generated file 
			for (String scenarioKey : scenario_data.keySet()) {
				String querybuilder = "";
				String query = null;
				for (String key : scenario_data.get(scenarioKey).keySet()) {
					if (!(key.equalsIgnoreCase("Scenario_ID"))) {
						querybuilder = key + "='" + scenario_data.get(scenarioKey).get(key) + "'" + " , "
								+ querybuilder;
					}
				}
				String test = querybuilder.substring(0, querybuilder.length() - 2);
				query = "Update Sheet Set " + test + "where Scenario_ID='" + scenarioKey + "'";
				mast.executeUpdate(query);
			}
			data.close();
			scen.close();
			mast.close();
		}
	}

	@SuppressWarnings("unchecked")
	public static void ReadData(String[] array, Connection mast, Connection data, Connection scen,
			ArrayList<String> alFields, ArrayList<String> recordData, HashMap<String, String> dataSheet,
			HashMap<String, String> scenSheet, HashMap<String, HashMap<String, String>> scenario_data,
			ArrayList<String> recordScen) {
		HashMap<String, String> temp = new HashMap<String, String>();
		HashMap<String, String> tmpo = new HashMap<String, String>();
		try {
			for (int i = 0; i < array.length; i++) {
				if (!array[i].isEmpty()) {
					String[] spli = array[i].split("=");
					String[] split = spli[1].split("#");
					splitColon = array[i].split("-");
					if (split[0].equalsIgnoreCase("static")) {
						Recordset recordsetdata;
						Recordset recordsetscen;
						String strQueryAll = "Select Scenario_ID from Sheet";
						Recordset recordsetmast = mast.executeQuery(strQueryAll);
						while (recordsetmast.next()) {
							ArrayList<String> colCollection = recordsetmast.getFieldNames();
							int Iter;
							int size = colCollection.size();
							for (Iter = 0; Iter <= (size - 1); Iter++) {
								String ColName = colCollection.get(Iter);
								testcasename = recordsetmast.getField(ColName);

								// Data_Sheet
								try {
									if (split[1].equalsIgnoreCase("DataSource")) {
										String strQuery2 = "Select " + splitColon[0] + " from Sheet Where Scenario_ID='" + testcasename.trim() + "'";
										recordsetdata = data.executeQuery(strQuery2);
										alFields = recordsetdata.getFieldNames();
										while (recordsetdata.next()) {
											for (String str : alFields) {
												dataSheet.put(str, recordsetdata.getField(str));
											}
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								try {
									if (split[1].equalsIgnoreCase("ScenarioSource")) {
										String strQuery3 = "Select " + splitColon[0] + " from Sheet Where Scenario_ID='" + testcasename.trim() + "'";
										recordsetscen = scen.executeQuery(strQuery3);
										alFields = recordsetscen.getFieldNames();
										while (recordsetscen.next()) {
											for (String str : alFields) {
												dataSheet.put(str, recordsetscen.getField(str));
											}
										}
									}

								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							if (i != 0) {
								temp = scenario_data.get(testcasename);
								temp.putAll(dataSheet);
								scenario_data.put(testcasename, (HashMap<String, String>) temp.clone());
								temp.clear();
								dataSheet.clear();
							}
							if (i == 0) {
								tmpo = (HashMap<String, String>) dataSheet.clone();
								scenario_data.put(testcasename, tmpo);
								dataSheet.clear();
							}
						}
					} else if (split[0].equalsIgnoreCase("Dynamic")) {
						String strQueryAll = "Select Scenario_ID from Sheet";
						Recordset recordsetmast = mast.executeQuery(strQueryAll);
						while (recordsetmast.next()) {
							ArrayList<String> colCollection = recordsetmast.getFieldNames();
							int Iter;
							int size = colCollection.size();
							for (Iter = 0; Iter <= (size - 1); Iter++) {
								String ColName = colCollection.get(Iter);
								testcasename = recordsetmast.getField(ColName);
								String randomvalue;
								String m = split[1];
								int s = Integer.parseInt(m);
								Random rand = new Random();
								if ("IFSC".equalsIgnoreCase(splitColon[0])) {
									String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
									StringBuilder salt = new StringBuilder();
									Random rnd = new Random();
									while (salt.length() < s) {
										int index = (int) (rnd.nextFloat() * SALTCHARS.length());
										salt.append(SALTCHARS.charAt(index));
									}
									String saltStr = salt.toString();
									// System.out.println(saltStr);
									randomvalue = saltStr;
								} else {
									String test = "";
									int n = 0;
									for (int k = 1; k <= s; k++) {
										n = rand.nextInt(k);
										n += 1;
										test = test + Integer.toString(n);
										// System.out.print(n);
									}
									randomvalue = test;
								}
								dataSheet.put(splitColon[0], randomvalue);
								if (i != 0) {
									temp = scenario_data.get(testcasename);
									temp.putAll(dataSheet);
									scenario_data.put(testcasename, (HashMap<String, String>) temp.clone());
									temp.clear();
									dataSheet.clear();
								}
								if (i == 0) {
									tmpo = (HashMap<String, String>) dataSheet.clone();
									scenario_data.put(testcasename, tmpo);
									dataSheet.clear();
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
