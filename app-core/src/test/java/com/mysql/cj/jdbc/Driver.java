package com.mysql.cj.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class Driver implements java.sql.Driver {

    public static final List<String> EXECUTED_SQL = new ArrayList<String>();

    static {
        try {
            DriverManager.registerDriver(new Driver());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        return createConnectionProxy();
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith("jdbc:mysql://");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(Driver.class.getName());
    }

    private Connection createConnectionProxy() {
        InvocationHandler handler = new InvocationHandler() {
            private boolean closed;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("createStatement".equals(name)) {
                    return createStatementProxy((Connection) proxy);
                }
                if ("close".equals(name)) {
                    closed = true;
                    return null;
                }
                if ("isClosed".equals(name)) {
                    return Boolean.valueOf(closed);
                }
                if ("setAutoCommit".equals(name) || "commit".equals(name) || "rollback".equals(name)) {
                    return null;
                }
                if ("getAutoCommit".equals(name)) {
                    return Boolean.TRUE;
                }
                if ("unwrap".equals(name)) {
                    return null;
                }
                if ("isWrapperFor".equals(name)) {
                    return Boolean.FALSE;
                }
                if ("toString".equals(name)) {
                    return "MockMySqlConnection";
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (Connection) Proxy.newProxyInstance(
                Driver.class.getClassLoader(),
                new Class[]{Connection.class},
                handler);
    }

    private Statement createStatementProxy(final Connection connection) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String name = method.getName();
                if ("executeUpdate".equals(name) || "execute".equals(name)) {
                    if (args != null && args.length > 0 && args[0] != null) {
                        EXECUTED_SQL.add(String.valueOf(args[0]));
                    }
                    if ("execute".equals(name)) {
                        return Boolean.TRUE;
                    }
                    return Integer.valueOf(0);
                }
                if ("close".equals(name)) {
                    return null;
                }
                if ("getConnection".equals(name)) {
                    return connection;
                }
                if ("unwrap".equals(name)) {
                    return null;
                }
                if ("isWrapperFor".equals(name)) {
                    return Boolean.FALSE;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (Statement) Proxy.newProxyInstance(
                Driver.class.getClassLoader(),
                new Class[]{Statement.class},
                handler);
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (returnType == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (returnType == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (returnType == Double.TYPE) {
            return Double.valueOf(0d);
        }
        if (returnType == Float.TYPE) {
            return Float.valueOf(0f);
        }
        if (returnType == Short.TYPE) {
            return Short.valueOf((short) 0);
        }
        if (returnType == Byte.TYPE) {
            return Byte.valueOf((byte) 0);
        }
        if (returnType == Character.TYPE) {
            return Character.valueOf((char) 0);
        }
        return null;
    }
}
