package com.fluxtream.admintools;

import java.sql.*;
import java.util.*;

/**
 * User: candide
 * Date: 15/05/13
 * Time: 10:33
 */
class CleanupNullApiKeyIds{

    public void run() throws SQLException {
        System.out.println("trying to connect using url: " + Main.props.getProperty("db.url"));

        Connection connect = Main.getConnection();

        cleanupStaleData(connect);

        Statement statement = connect.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM ApiKey");
        Map<String,Long> apiKeys = new HashMap<String,Long>();
        while(resultSet.next()) {
            final int api = resultSet.getInt("api");
            final long guestId = resultSet.getLong("guestId");
            final long apiKeyId = resultSet.getLong("id");
            apiKeys.put(String.format("%s_%s", api, guestId), apiKeyId);
        }

        String list = "Facet_BodymediaBurn " +
                "Facet_BodymediaSleep " +
                "Facet_BodymediaSteps " +
                "Facet_CalendarEventEntry " +
                "Facet_FitbitActivity " +
                "Facet_FitbitLoggedActivity " +
                "Facet_FitbitSleep " +
                "Facet_FitbitWeight " +
                "Facet_FlickrPhoto " +
                "Facet_FluxtreamCapturePhoto " +
                "Facet_GithubPush " +
                "Facet_LastFmLovedTrack " +
                "Facet_LastFmRecentTrack " +
                "Facet_Location " +
                "Facet_MymeeObservation " +
                "Facet_QuantifiedMindTest " +
                "Facet_RunKeeperFitnessActivity " +
                "Facet_Tweet " +
                "Facet_TwitterDirectMessage " +
                "Facet_TwitterMention " +
                "Facet_WithingsBPMMeasure " +
                "Facet_WithingsBodyScaleMeasure " +
                "Facet_ZeoSleepStats";
        StringTokenizer st = new StringTokenizer(list);
        List<String> tableNames = new ArrayList<String>();
        while(st.hasMoreTokens())
            tableNames.add(st.nextToken());
        for (String tableName : tableNames) {
	    System.out.println("Processing " + tableName);

            resultSet = statement.executeQuery(String.format("select guestId, api from %s where apiKeyId is null group by guestId,api;", tableName));
            PreparedStatement pstmt = connect.prepareStatement(String.format("UPDATE %s SET apiKeyId=? WHERE api=? AND guestId=?", tableName));
            while(resultSet.next()) {
                final int api = resultSet.getInt("api");
                final long guestId = resultSet.getLong("guestId");
                Long apiKeyId = apiKeys.get(String.format("%s_%s", api, guestId));

                if (apiKeyId==null) {
                    System.out.println(String.format("  No apiKey for table %s api=%s, guestId=%s. To cleanup, execute:\n" +
						     "    DELETE from %s where api=%s and guestId=%s;", 
						     tableName, api, guestId, tableName, api, guestId));
                    continue;
                }
                pstmt.setLong(1, apiKeyId);
                pstmt.setInt(2, api);
                pstmt.setLong(3, guestId);
                final int rowsUpdated = pstmt.executeUpdate();
                System.out.println("  " + rowsUpdated + " " + tableName + " rows have been assigned an apiKeyId");
            }
        }
    }

    public void cleanupStaleData(Connection connect) throws SQLException {
        long oneDayAgo = System.currentTimeMillis() - 86400000l;
        Statement statement = connect.createStatement();
        final int updateWorkerTasksDeleted = statement.executeUpdate(String.format("DELETE FROM UpdateWorkerTask WHERE not(status=2 AND updateType=2) and timeScheduled<%s", oneDayAgo));
        System.out.println("deleted " + updateWorkerTasksDeleted + " UpdateWorkerTasks");
        final int apiUpdatesDeleted = statement.executeUpdate(String.format("DELETE FROM ApiUpdates WHERE ts<%s", oneDayAgo));
        System.out.println("deleted " + apiUpdatesDeleted + " ApiUpdates");
    }

}