package servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.sql.*;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet
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

        // 获取到用户登录请求中的账号、密码信息
        String username = request.getParameter("username");
        String password = request.getParameter("userpass");

        // 连接数据库用户表进行身份验证
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean loginSuccess = false;
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

            // 4.创建数据库操作命令对象
            String sql = "SELECT * FROM users WHERE user_name = ? AND user_pass = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            // 5.使用数据库操作命令对象向数据库发送命令（SQL语句），并接收返回数据集
            rs = stmt.executeQuery();

            // 6.对返回的数据集进行分析和处理，进行业务逻辑的处理
            loginSuccess = rs.next();
        } catch (ClassNotFoundException e)
        {
            request.setAttribute("errorMessage", "数据库驱动加载失败：" + e.getMessage());
        } catch (SQLException e)
        {
            request.setAttribute("errorMessage", "数据库操作出错：" + e.getMessage());
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
                request.setAttribute("errorMessage", "关闭数据库资源出错：" + e.getMessage());
            }
        }

        // 将账号和密码信息存储到请求属性中
        request.setAttribute("username", username);
        request.setAttribute("password", password);
        if (loginSuccess)
        {
            // 登录成功，跳转到成功页面
            String redirectUrl = "login/login_success.html?username=" + username + "&password=" + password;
            response.sendRedirect(redirectUrl);
        } else
        {
            // 登录失败，跳转到错误页面
            String redirectUrl = "login/login_unsuccess.html?username=" + username + "&password=" + password;
            response.sendRedirect(redirectUrl);
        }
    }
}