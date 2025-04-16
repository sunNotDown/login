package servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet
{
    private static final String DATABASE_PROPERTIES = "database.properties";
    private Properties dataProperties;


    @Override
    public void init() throws ServletException
    {
        super.init();
        dataProperties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(DATABASE_PROPERTIES))
        {
            if (inputStream != null)
            {
                dataProperties.load(inputStream);
            } else
            {
                throw new ServletException("无法加载配置文件: " + DATABASE_PROPERTIES);
            }
        } catch (IOException e)
        {
            throw new ServletException("加载配置文件时出错", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html;charset=utf-8");

        // 获取到用户注册请求中的账号、密码信息
        String username = request.getParameter("reg_username");
        String password = request.getParameter("reg_initial_password");
        String error = "未知错误";

        // 连接数据库用户表进行注册验证
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean registerSuccess = false;
        try
        {
            // 1，准备访问参数（数据库的连接字符串，访问账号，访问密码，数据库驱动主类名）
            String url = dataProperties.getProperty("db.url");
            String dbUsername = dataProperties.getProperty("db.username");
            String dbPassword = dataProperties.getProperty("db.password");
            String driverClassName = dataProperties.getProperty("db.driver");

            // 2.加载数据库驱动（反射机制）
            Class.forName(driverClassName);

            // 3.连接数据库，并获取数据库连接对象
            conn = DriverManager.getConnection(url, dbUsername, dbPassword);

            // 检查用户名是否已存在
            String checkSql = "SELECT * FROM users WHERE user_name = ?";
            stmt = conn.prepareStatement(checkSql);
            stmt.setString(1, username);
            rs = stmt.executeQuery();

            if (rs.next())
            {
                error = "用户名已存在，请选择其他用户名！";
            } else
            {
                // 用户名不存在，进行注册操作
                String insertSql = "INSERT INTO users (user_name, user_pass) VALUES (?,?)";
                stmt = conn.prepareStatement(insertSql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                int rows = stmt.executeUpdate();
                if (rows > 0)
                {
                    registerSuccess = true;
                } else
                {
                    error = "注册失败，请稍后再试！";
                }
            }
        } catch (ClassNotFoundException e)
        {
            error = "数据库驱动加载失败";
        } catch (SQLException e)
        {
            error = "数据库操作出错";
        } finally
        {
            // 7.释放数据库资源，关闭数据库连接
            try
            {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e)
            {
                error = "关闭数据库资源出错：";
            }
        }


        if (registerSuccess)
        {
            // 注册成功，跳转到成功页面
            String redirectUrl = "login/register_success.html?username=" + username + "&password=" + password;
            response.sendRedirect(redirectUrl);
        } else
        {
            // 注册失败，跳转到错误页面
            String redirectUrl = "login/register_unsuccess.html";
            response.sendRedirect(redirectUrl);
        }
    }
}