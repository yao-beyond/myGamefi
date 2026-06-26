package com.example.app.controller;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.example.app.bo.PayoutBO;
import com.example.app.dto.Payout;

/**
 * CPS-06 · Controller：@WebServlet 入口，只委派 BO，禁止碰 DAO/Cache。
 */
@WebServlet(urlPatterns = "/customer/payoutController/*")
public final class PayoutController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        switch (pathInfo) {
            case "/request":
                long id = PayoutBO.requestPayout(
                        Long.parseLong(request.getParameter("customerId")),
                        Long.parseLong(request.getParameter("amount")));
                response.getWriter().write("{\"payoutId\":" + id + "}");
                break;
            case "/get":
                Payout p = PayoutBO.getPayout(Long.parseLong(request.getParameter("id")));
                response.setContentType("application/json");
                response.getWriter().write(toJson(p));
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String toJson(Payout p) {
        if (p == null) {
            return "null";
        }
        return "{\"id\":" + p.getId() + ",\"amount\":" + p.getAmount()
                + ",\"status\":" + p.getStatusValue() + "}";
    }
}
