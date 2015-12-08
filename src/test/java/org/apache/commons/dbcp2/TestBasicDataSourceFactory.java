/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.junit.Test;

/**
 * TestSuite for BasicDataSourceFactory
 *
 * @author Dirk Verbeeck
 * @version $Id$
 */
public class TestBasicDataSourceFactory {

    private static AtomicInteger instanceCounter = new AtomicInteger(0);

    @Test
    public void testNoProperties() throws Exception {
        Properties properties = new Properties();
        BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties);

        assertNotNull(ds);
    }

    @Test
    public void testProperties() throws Exception {
        BasicDataSource ds = BasicDataSourceFactory.createDataSource(getTestProperties());
        checkDataSourceProperties(ds);
    }

    @Test
    public void testValidateProperties() throws Exception {
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            final Reference ref = new Reference("javax.sql.DataSource",
                                          BasicDataSourceFactory.class.getName(), null);
            ref.add(new StringRefAddr("foo", "bar"));     // Unknown
            ref.add(new StringRefAddr("maxWait", "100")); // Changed
            ref.add(new StringRefAddr("driverClassName", "org.apache.commons.dbcp2.TesterDriver")); //OK
            final BasicDataSourceFactory basicDataSourceFactory = new BasicDataSourceFactory();
            basicDataSourceFactory.getObjectInstance(ref, null, null, null);
            final List<String> messages = StackMessageLog.getAll();
            assertEquals(2,messages.size());
            for (String message : messages) {
                if (message.contains("maxWait")) {
                    assertTrue(message.contains("use maxWaitMillis"));
                } else {
                    assertTrue(message.contains("foo"));
                    assertTrue(message.contains("Ignoring unknown property"));
                }
            }
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    @Test
    public void testAllProperties() throws Exception {
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            final Reference ref = new Reference("javax.sql.DataSource",
                                          BasicDataSourceFactory.class.getName(), null);
            Properties properties = getTestProperties();
            for (Entry<Object, Object> entry : properties.entrySet()) {
                ref.add(new StringRefAddr((String) entry.getKey(), (String) entry.getValue()));
            }
            final BasicDataSourceFactory basicDataSourceFactory = new BasicDataSourceFactory();
            BasicDataSource ds = (BasicDataSource) basicDataSourceFactory.getObjectInstance(ref, null, null, null);
            checkDataSourceProperties(ds);
            final List<String> messages = StackMessageLog.getAll();
            assertEquals(0,messages.size());
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    private Properties getTestProperties() {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.apache.commons.dbcp2.TesterDriver");
        properties.setProperty("url", "jdbc:apache:commons:testdriver");
        properties.setProperty("maxTotal", "10");
        properties.setProperty("maxIdle", "8");
        properties.setProperty("minIdle", "0");
        properties.setProperty("maxWaitMillis", "500");
        properties.setProperty("initialSize", "5");
        properties.setProperty("defaultAutoCommit", "true");
        properties.setProperty("defaultReadOnly", "false");
        properties.setProperty("defaultTransactionIsolation", "READ_COMMITTED");
        properties.setProperty("defaultCatalog", "test");
        properties.setProperty("testOnBorrow", "true");
        properties.setProperty("testOnReturn", "false");
        properties.setProperty("username", "username");
        properties.setProperty("password", "password");
        properties.setProperty("validationQuery", "SELECT DUMMY FROM DUAL");
        properties.setProperty("validationQueryTimeout", "100");
        properties.setProperty("connectionInitSqls", "SELECT 1;SELECT 2");
        properties.setProperty("timeBetweenEvictionRunsMillis", "1000");
        properties.setProperty("minEvictableIdleTimeMillis", "2000");
        properties.setProperty("softMinEvictableIdleTimeMillis", "3000");
        properties.setProperty("numTestsPerEvictionRun", "2");
        properties.setProperty("testWhileIdle", "true");
        properties.setProperty("accessToUnderlyingConnectionAllowed", "true");
        properties.setProperty("removeAbandonedOnBorrow", "true");
        properties.setProperty("removeAbandonedOnMaintenance", "true");
        properties.setProperty("removeAbandonedTimeout", "3000");
        properties.setProperty("logAbandoned", "true");
        properties.setProperty("abandonedUsageTracking", "true");
        properties.setProperty("poolPreparedStatements", "true");
        properties.setProperty("maxOpenPreparedStatements", "10");
        properties.setProperty("lifo", "true");
        properties.setProperty("fastFailValidation", "true");
        properties.setProperty("disconnectionSqlCodes", "XXX,YYY");
        properties.setProperty("jmxName", "org.apache.commons.dbcp2:name=test");
        return properties;
    }

    private void checkDataSourceProperties(BasicDataSource ds) throws Exception {
        assertEquals("org.apache.commons.dbcp2.TesterDriver", ds.getDriverClassName());
        assertEquals("jdbc:apache:commons:testdriver", ds.getUrl());
        assertEquals(10, ds.getMaxTotal());
        assertEquals(8, ds.getMaxIdle());
        assertEquals(0, ds.getMinIdle());
        assertEquals(500, ds.getMaxWaitMillis());
        assertEquals(5, ds.getInitialSize());
        assertEquals(5, ds.getNumIdle());
        assertEquals(Boolean.TRUE, ds.getDefaultAutoCommit());
        assertEquals(Boolean.FALSE, ds.getDefaultReadOnly());
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, ds.getDefaultTransactionIsolation());
        assertEquals("test", ds.getDefaultCatalog());
        assertEquals(true, ds.getTestOnBorrow());
        assertEquals(false, ds.getTestOnReturn());
        assertEquals("username", ds.getUsername());
        assertEquals("password", ds.getPassword());
        assertEquals("SELECT DUMMY FROM DUAL", ds.getValidationQuery());
        assertEquals(100, ds.getValidationQueryTimeout());
        assertEquals(2, ds.getConnectionInitSqls().size());
        assertEquals("SELECT 1", ds.getConnectionInitSqls().get(0));
        assertEquals("SELECT 2", ds.getConnectionInitSqls().get(1));
        assertEquals(1000, ds.getTimeBetweenEvictionRunsMillis());
        assertEquals(2000, ds.getMinEvictableIdleTimeMillis());
        assertEquals(3000, ds.getSoftMinEvictableIdleTimeMillis());
        assertEquals(2, ds.getNumTestsPerEvictionRun());
        assertEquals(true, ds.getTestWhileIdle());
        assertEquals(true, ds.isAccessToUnderlyingConnectionAllowed());
        assertEquals(true, ds.getRemoveAbandonedOnBorrow());
        assertEquals(true, ds.getRemoveAbandonedOnMaintenance());
        assertEquals(3000, ds.getRemoveAbandonedTimeout());
        assertEquals(true, ds.getLogAbandoned());
        assertEquals(true, ds.getAbandonedUsageTracking());
        assertEquals(true, ds.isPoolPreparedStatements());
        assertEquals(10, ds.getMaxOpenPreparedStatements());
        assertEquals(true, ds.getLifo());
        assertEquals(true, ds.getFastFailValidation());
        assertTrue(ds.getDisconnectionSqlCodes().contains("XXX"));
        assertTrue(ds.getDisconnectionSqlCodes().contains("YYY"));
        assertEquals("org.apache.commons.dbcp2:name=test", ds.getJmxName());

        // Unregister so subsequent calls to getTestProperties can re-register
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbs.unregisterMBean(ds.getRegisteredJmxName());
    }
}
