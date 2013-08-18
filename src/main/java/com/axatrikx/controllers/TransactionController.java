package com.axatrikx.controllers;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.axatrikx.beans.TransactionItem;
import com.axatrikx.db.DatabaseController;
import com.axatrikx.errors.DataBaseException;
import com.axatrikx.errors.DatabaseTableCreationException;
import com.axatrikx.utils.CommonSettings;
import com.axatrikx.utils.ConfigValues;
import com.axatrikx.utils.PreparedDataExecutor;

public class TransactionController {

	private static Logger log = Logger.getLogger(TransactionController.class);

	private static final String INSERT_TRANS_TABLE_TKN = "INSERT_TRANS_TABLE";
	private static final String INSERT_ITEMS_TABLE_TKN = "INSERT_ITEMS_TABLE";

	public void getTransactionModel(String queryString) {

	}

	/**
	 * Returns the select query based on the type of query. Types include 'QUERY_TRANS_TABLE', 'QUERY_ITEMS_TABLE',
	 * QUERY_ITEM_WITH_NAME and 'QUERY_TRANS_DETAIL'.
	 * 
	 * @param queryType
	 *            Which query to return. Types include 'QUERY_TRANS_TABLE', 'QUERY_ITEMS_TABLE', QUERY_ITEM_WITH_NAME
	 *            and 'QUERY_TRANS_DETAIL'.
	 * @return The query string.
	 */
	public static String getDBSelectQuery(String queryType) {
		String result = null;
		Properties queryValues = CommonSettings.getPropertiesFromFile(ConfigValues.QUERY_FOLDER.toString()
				+ ConfigValues.SEPARATOR.toString() + ConfigValues.QUERY_SELECT_FILE.toString());
		result = queryValues.getProperty(queryType);
		if (result.isEmpty()) {
			log.error("Couldnt find date for : " + queryType);
		}
		return result;
	}

	/**
	 * Returns the insert query based on the type of query. Types include 'INSERT_TRANS_TABLE', 'INSERT_ITEMS_TABLE'.
	 * 
	 * @param queryType
	 *            Which query to return. Types include 'INSERT_TRANS_TABLE', 'INSERT_ITEMS_TABLE'.
	 * @return The query string.
	 */
	public static String getDBInsertQuery(String tableType) {
		String result = null;
		Properties queryValues = CommonSettings.getPropertiesFromFile(ConfigValues.QUERY_FOLDER.toString()
				+ ConfigValues.SEPARATOR.toString() + ConfigValues.QUERY_INSERT_FILE.toString());
		result = queryValues.getProperty(tableType);
		if (result.isEmpty()) {
			log.error("Invalid tableType: " + tableType);
		}
		return result;
	}

	/**
	 * Inserts the provided transaction to Database. If the transaction item is new, it will also be added to Database.
	 * 
	 * @param buyerName
	 * @param location
	 * @param cost
	 * @param price
	 * @param date
	 * @param itemName
	 * @param category
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws DataBaseException
	 * @throws DatabaseTableCreationException
	 */
	@SuppressWarnings("rawtypes")
	public boolean insertTransaction(String buyerName, String location, float cost, float price, Date date,
			String itemName, String category, float rate) throws ClassNotFoundException, SQLException,
			DataBaseException, DatabaseTableCreationException {

		TransactionItem item = processTransactionItem(itemName, category, rate);

		ArrayList<HashMap<Class, Object>> dataList = new ArrayList<HashMap<Class, Object>>();

		HashMap<Class, Object> itemIdVal = new HashMap<Class, Object>();
		itemIdVal.put(Integer.class, item.getItemId());
		dataList.add(itemIdVal);

		HashMap<Class, Object> buyerNameVal = new HashMap<Class, Object>();
		buyerNameVal.put(String.class, buyerName);
		dataList.add(buyerNameVal);

		HashMap<Class, Object> locationVal = new HashMap<Class, Object>();
		locationVal.put(String.class, location);
		dataList.add(locationVal);

		HashMap<Class, Object> costVal = new HashMap<Class, Object>();
		costVal.put(Float.class, cost);
		dataList.add(costVal);

		HashMap<Class, Object> priceVal = new HashMap<Class, Object>();
		priceVal.put(Float.class, price);
		dataList.add(priceVal);

		HashMap<Class, Object> profitVal = new HashMap<Class, Object>();
		profitVal.put(Float.class, 10.0f); // TODO calculate profit
		dataList.add(profitVal);

		HashMap<Class, Object> dateVal = new HashMap<Class, Object>();
		dateVal.put(Timestamp.class, new Timestamp(date.getTime()));
		dataList.add(dateVal);

		int noOfRowsAffected = new PreparedDataExecutor(new DatabaseController().getConnection(), dataList,
				getDBInsertQuery(INSERT_TRANS_TABLE_TKN).replace(DatabaseController.getDatabaseNameToken(),
						DatabaseController.getDatabaseName())).getPreparedStatement().executeUpdate();
		boolean isSuccessful = false;
		if (noOfRowsAffected > 0) {
			isSuccessful = true;
		}
		return isSuccessful;
	}

	@SuppressWarnings("rawtypes")
	private TransactionItem processTransactionItem(String itemName, String category, float rate)
			throws ClassNotFoundException, SQLException, DataBaseException, DatabaseTableCreationException {
		TransactionItem item = new TransactionItemController().getItem(itemName);
		if (item == null) {
			log.info("Item " + itemName + " doesn't exist. Creating item record.");

			ArrayList<HashMap<Class, Object>> dataList = new ArrayList<HashMap<Class, Object>>();

			HashMap<Class, Object> itemNameVal = new HashMap<Class, Object>();
			itemNameVal.put(String.class, itemName);
			dataList.add(itemNameVal);

			HashMap<Class, Object> categoryVal = new HashMap<Class, Object>();
			categoryVal.put(String.class, category);
			dataList.add(categoryVal);

			HashMap<Class, Object> rateVal = new HashMap<Class, Object>();
			rateVal.put(Float.class, rate);
			dataList.add(rateVal);

			int noOfRowsAffected = new PreparedDataExecutor(new DatabaseController().getConnection(), dataList,
					getDBInsertQuery(INSERT_ITEMS_TABLE_TKN).replace(DatabaseController.getDatabaseNameToken(),
							DatabaseController.getDatabaseName())).getPreparedStatement().executeUpdate();

			if (noOfRowsAffected > 0) {
				log.info("New item '" + itemName + "' created");
				item = new TransactionItemController().getItem(itemName);
			} else {
				log.error("Failed to add item " + itemName + "'");
			}
		}
		return item;
	}
}
