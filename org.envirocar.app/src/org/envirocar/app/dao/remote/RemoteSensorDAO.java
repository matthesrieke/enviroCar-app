/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.dao.remote;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.envirocar.app.activity.preference.CarSelectionPreference;
import org.envirocar.app.application.ECApplication;
import org.envirocar.app.dao.NotConnectedException;
import org.envirocar.app.dao.SensorDAO;
import org.envirocar.app.dao.SensorRetrievalException;
import org.envirocar.app.dao.cache.CacheSensorDAO;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Car;
import org.envirocar.app.network.HTTPClient;
import org.json.JSONException;
import org.json.JSONObject;

public class RemoteSensorDAO extends BaseRemoteDAO implements SensorDAO, AuthenticatedDAO {
	
	static final Logger logger = Logger.getLogger(RemoteSensorDAO.class);
	private CacheSensorDAO cache;

	public RemoteSensorDAO(CacheSensorDAO cacheSensorDAO) {
		this.cache = cacheSensorDAO;
	}

	@Override
	public List<Car> getAllSensors() throws SensorRetrievalException {
		
		try {
			String content = HTTPClient.executeAndParseJsonRequest(ECApplication.BASE_URL+"/sensors");
		
			if (cache != null) {
				try {
					cache.storeAllSensors(content);
				}
				catch (IOException e) {
					logger.warn(e.getMessage());
				}
			}
			
			JSONObject parentObject = new JSONObject(content);
			
			return Car.fromJsonList(parentObject);
		} catch (IOException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		} catch (JSONException e) {
			logger.warn(e.getMessage());
			throw new SensorRetrievalException(e);
		}
	}

	@Override
	public String saveSensor(Car car) throws NotConnectedException {
		String sensorString = String
				.format(Locale.ENGLISH,
						"{ \"type\": \"%s\", \"properties\": {\"manufacturer\": \"%s\", \"model\": \"%s\", \"fuelType\": \"%s\", \"constructionYear\": %s, \"engineDisplacement\": %s } }",
						CarSelectionPreference.SENSOR_TYPE, car.getManufacturer(), car.getModel(), car.getFuelType(),
						car.getConstructionYear(), car.getEngineDisplacement());
		try {
			return registerSensor(sensorString);
		} catch (IOException e) {
			throw new NotConnectedException(e);
		}
	}
	
	private String registerSensor(String sensorString) throws IOException, NotConnectedException {
		
		HttpPost postRequest = new HttpPost(
				ECApplication.BASE_URL+"/sensors");
		
		postRequest.addHeader("Content-Type", "application/json");
		
		postRequest.addHeader("Accept-Encoding", "gzip");
		
		StringEntity se = new StringEntity(sensorString);
		se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
		
		postRequest.setEntity(se);
		
		HttpResponse response = super.executeHttpRequest(postRequest);
		
		Header[] h = response.getAllHeaders();

		String location = "";
		for (int i = 0; i < h.length; i++) {
			if (h[i].getName().equals("Location")) {
				location += h[i].getValue();
				break;
			}
		}
		logger.info("location: "
				+location);

		return location.substring(
				location.lastIndexOf("/") + 1,
				location.length());
	}

}
