package it.univaq.seas.daoImpl;

import it.univaq.seas.dao.RoomDao;
import it.univaq.seas.model.RoomData;
import it.univaq.seas.model.RoomDataRegression;
import it.univaq.seas.utility.Utility;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.ArrayList;
import java.util.List;

import static it.univaq.seas.utility.Utility.booleanCast;
import static it.univaq.seas.utility.Utility.intcast;

public class RoomDaoImpl implements RoomDao {
    private String serverURL,username,password;

    public RoomDaoImpl(){
        serverURL = (Utility.dockerized) ?  "http://influxdb:8086" : "http://localhost:8086";
        username = "telegraf";
        password = "secretpassword";
    }

    @Override
    public List<RoomData> getRoomData() {
        InfluxDB influxDB = InfluxDBFactory.connect(serverURL, username, password);
        String command = "SELECT last(*) FROM room WHERE time >= now() - 7d GROUP BY topic";
        QueryResult queryResult = influxDB.query(new Query(command,"telegraf"));
        List<RoomData> rooms = new ArrayList<>();

        for(QueryResult.Result series : queryResult.getResults()) {
            for (QueryResult.Series singleSerie : series.getSeries()) {
                List<Object> tuple = singleSerie.getValues().get(0);
                RoomData temporaryRoom  = new RoomData();
                int roomId = intcast(tuple.get(6));
                if(roomId != 0) {
                    temporaryRoom.setRoomName("room"+roomId);
                    temporaryRoom.setBatteryCapacity(intcast(tuple.get(1)));
                    temporaryRoom.setBatteryInput(intcast(tuple.get(2)));
                    temporaryRoom.setBatteryLevel(intcast(tuple.get(3)));
                    temporaryRoom.setBatteryOutput(intcast(tuple.get(4)));
                    temporaryRoom.setEnergyDemand(intcast(tuple.get(5)));
                    temporaryRoom.setRoomId(roomId);
                    temporaryRoom.setSockets(intcast(tuple.get(7)));
                    temporaryRoom.setStatus(booleanCast(tuple.get(8)));
                    temporaryRoom.setTopic(singleSerie.getTags().get("topic"));
                    rooms.add(temporaryRoom);
                }
            }
        }
        return rooms;
    }

    @Override
    public List<RoomDataRegression> getRoomDataRegression() {
        InfluxDB influxDB = InfluxDBFactory.connect(serverURL, username, password);
        String command = "SELECT last(*) FROM room WHERE time >= now() - 7d GROUP BY topic";
        QueryResult queryResult = influxDB.query(new Query(command,"telegraf"));
        List<RoomDataRegression> roomDataRegressions = new ArrayList<>();
        for(QueryResult.Result series: queryResult.getResults()){
            for (QueryResult.Series singleSerie : series.getSeries()){
                List<Object> tuple = singleSerie.getValues().get(0);
                RoomDataRegression temporaryRoom = new RoomDataRegression();
                int roomId = intcast(tuple.get(6));
                String temporaryTopic = singleSerie.getTags().get("topic");
                if(roomId != 0) {
                    temporaryRoom.setRoomName("room"+roomId);
                    temporaryRoom.setBatteryCapacity(intcast(tuple.get(1)));
                    temporaryRoom.setBatteryInput(intcast(tuple.get(2)));
                    temporaryRoom.setBatteryLevel(intcast(tuple.get(3)));
                    temporaryRoom.setBatteryOutput(intcast(tuple.get(4)));
                    temporaryRoom.setEnergyDemand(intcast(tuple.get(5)));
                    temporaryRoom.setRoomId(roomId);
                    temporaryRoom.setSockets(intcast(tuple.get(7)));
                    temporaryRoom.setStatus(booleanCast(tuple.get(8)));
                    temporaryRoom.setTopic(temporaryTopic);
                    temporaryRoom.setEnergyDemandHistory(getMeanEnergyDemand(temporaryTopic,influxDB));
                    roomDataRegressions.add(temporaryRoom);
                }
            }
        }
        return roomDataRegressions;
    }

    private List<Integer> getMeanEnergyDemand(String topic, InfluxDB influxDB) {
        String newCommand = "SELECT mean(energyDemand) FROM room WHERE topic = '" + topic + "' GROUP BY time(10m)";
        QueryResult newresult = influxDB.query(new Query(newCommand, "telegraf"));
        List<Integer> demands = new ArrayList<>();
        for(QueryResult.Result series : newresult.getResults()) {
            for (QueryResult.Series singleSerie : series.getSeries()) {
                for (List<Object> tuple : singleSerie.getValues()) {
                    demands.add(intcast(tuple.get(1)));
                }
            }
        }
        return demands;
    }
}
