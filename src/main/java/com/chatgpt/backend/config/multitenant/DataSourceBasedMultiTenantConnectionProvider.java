package com.chatgpt.backend.config.multitenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.Stoppable;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataSourceBasedMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String>, Stoppable {

    private final DataSource dataSource;

    public DataSourceBasedMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("SET search_path TO public");
        } catch (SQLException ignored) {}
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        final Connection connection = getAnyConnection();
        try (Statement st = connection.createStatement()) {
            st.execute(String.format("SET search_path TO \"%s\"", tenantIdentifier));
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("SET search_path TO public");
        } catch (SQLException ignored) {}
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return false;
    }

    @Override
    public <U> U unwrap(Class<U> unwrapType) {
        return null;
    }

    @Override
    public void stop() {
        // Nada que cerrar
    }
}
