package com.example.scaffold.cps06;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;

/**
 * CPS-06 · Controller 層（對應 MyXchange 的 ReportController）。
 *
 * 不變式：
 *  - @WebServlet 入口；以 pathInfo 分派到方法。
 *  - 只呼叫 BO，禁止 import / 直接呼叫 DAO 或 Cache。
 *  - 不含業務邏輯（解析 request → 委派 BO → 組裝 response）。
 */
@WebServlet(urlPatterns = "/member/orderController/*")
public final class OrderController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        switch (pathInfo) {
            case "/getOrder":
                getOrder(request, response);
                break;
            case "/advanceStatus":
                advanceStatus(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void getOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long id = Long.parseLong(request.getParameter("id"));
        Order order = OrderBO.getOrder(id);          // 只委派 BO
        response.setContentType("application/json");
        response.getWriter().write(toJson(order));
    }

    private void advanceStatus(HttpServletRequest request, HttpServletResponse response) {
        long id = Long.parseLong(request.getParameter("id"));
        int status = Integer.parseInt(request.getParameter("status"));
        OrderBO.advanceStatus(id, status);
    }

    private String toJson(Order order) {
        if (order == null) {
            return "null";
        }
        return "{\"id\":" + order.getId()
                + ",\"amount\":" + order.getAmount()
                + ",\"status\":" + order.getStatusValue() + "}";
    }
}
