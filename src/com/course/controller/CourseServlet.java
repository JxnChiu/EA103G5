package com.course.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.course.model.*;
import com.video.model.*;

@MultipartConfig
public class CourseServlet extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doPost(req, res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		req.setCharacterEncoding("UTF-8");
		String action = req.getParameter("action");
		System.out.println("***action = " + action);

		if ("getOne_For_Display".equals(action)) {// 取得特定物件

			List<String> errorMsgs = new LinkedList<String>();
			req.setAttribute("errorMsgs", errorMsgs);

			try {
				/*************************** 1.接收請求參數 - 輸入格式的錯誤處理 **********************/
				String courseno = req.getParameter("courseno");

				// 檢查空字串或空白
				if (courseno == null || courseno.trim().length() == 0) {
					errorMsgs.add("請輸入課程編號");
				}
				if (!errorMsgs.isEmpty()) {
					RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/select_page.jsp");
					failureView.forward(req, res);
					return; // 中斷程式
				}

				// 檢查流水碼編號格式
				String coursenoRegex = "^COUR[0-9]{4}$";
				if (!courseno.matches(coursenoRegex)) {
					errorMsgs.add("教室編號格式不正確");
				}
				if (!errorMsgs.isEmpty()) {
					RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/select_page.jsp");
					failureView.forward(req, res);
					return; // 中斷程式
				}

				/*************************** 2.開始查詢資料 *****************************************/
				CourseService courseSvc = new CourseService();
				CourseVO courseVO = courseSvc.getOneCourse(courseno);

				if (courseVO == null) {
					errorMsgs.add("查無資料");
				}
				if (!errorMsgs.isEmpty()) {
					RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/select_page.jsp");
					failureView.forward(req, res);
					return; // 中斷程式
				}

				/*************************** 3.查詢完成,準備轉交(Send the Success view) *************/
				req.setAttribute("courseVO", courseVO);

//				String url = "/front-end/course/listOneCourse.jsp";
				String url = "/front-end/course/mainCoursePage.jsp";
				RequestDispatcher successView = req.getRequestDispatcher(url);
				successView.forward(req, res);

				/*************************** 其他可能的錯誤處理 *************************************/
			} catch (Exception e) {
				errorMsgs.add("無法取得資料: " + e.getMessage());
				RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/select_page.jsp");
				failureView.forward(req, res);
			}
		}

		if ("getOneCourseForUpdate".equals(action)) {

			List<String> errorMsgs = new LinkedList<String>();
			req.setAttribute("errorMsgs", errorMsgs);

			try {
				/*************************** 1.接收請求參數 ****************************************/
				String courseno = req.getParameter("courseno");

				/*************************** 2.開始查詢資料 ****************************************/
				CourseService courseSvc = new CourseService();
				CourseVO courseVO = courseSvc.getOneCourse(courseno);

				/*************************** 3.查詢完成,準備轉交(Send the Success view) ************/
				req.setAttribute("courseVO", courseVO);
				String url = "/front-end/course/editCourse.jsp";
				RequestDispatcher successView = req.getRequestDispatcher(url);
				successView.forward(req, res);

				/*************************** 其他可能的錯誤處理 **********************************/
			} catch (Exception e) {
				errorMsgs.add("無法取得要修改的資料" + e.getMessage());
				RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/listAllCourse.jsp");
				failureView.forward(req, res);
				return;
			}
		}

		if ("update".equals(action)) {

			List<String> errorMsgs = new LinkedList<String>();
			req.setAttribute("errorMsgs", errorMsgs);

			try {
				/*********************** 1.接收請求參數 - 輸入格式的錯誤處理 *************************/
				String courseno = req.getParameter("courseno");
				String cstypeno = req.getParameter("cstypeno");
				String tchrno = req.getParameter("tchrno");

				String coursename = req.getParameter("coursename");
				String coursenameRegex = "^[(\u4e00-\u9fa5)(a-zA-Z0-9_ )]{2,50}$";
				if (coursename == null || coursename.trim().length() == 0) {
					coursename = "";
					errorMsgs.add("課程名稱請勿空白");
				} else if (!coursename.trim().matches(coursenameRegex)) {
					errorMsgs.add("課程名稱: 只能是中、英文字母、數字和_ , 且長度必需在2到50之間");
				}

				String courseinfo = req.getParameter("courseinfo");

				Integer courseprice = null;
				try {
					courseprice = Integer.valueOf(req.getParameter("courseprice"));
					if (courseprice <= 0) {
						errorMsgs.add("課程單價請輸入大於零的整數");
					}
				} catch (NumberFormatException e) {
					courseprice = 0;
					errorMsgs.add("課程單價請輸入數字");
				}

//				Integer ttltime = null;
//				// 應該要自己抓影片的長度
//				try {
//					ttltime = Integer.valueOf(req.getParameter("ttltime"));
//					if (ttltime <= 0) {
//						errorMsgs.add("課程總時數請輸入大於零的整數");
//					}
//				} catch (NumberFormatException e) {
//					ttltime = 0;
//					errorMsgs.add("課程總時數請輸入數字");
//				}

				Integer ttltime = 0;
				Map<String, String[]> map = new TreeMap<String, String[]>();
				map.put("courseno", new String[] { courseno });
				VideoService videoSvc = new VideoService();
				List<VideoVO> videoList = videoSvc.getAll(map);
				for (VideoVO videoVO : videoList) {
					ttltime += videoVO.getChapterlen();
				}

				String csstatus = req.getParameter("csstatus");

				Integer csscore = null;
				try {
					csscore = Integer.valueOf(req.getParameter("csscore"));
					if (csscore <= 0) {
						errorMsgs.add("累積總評分數請輸入大於零的整數");
					}
				} catch (NumberFormatException e) {
					csscore = 0;
					errorMsgs.add("累積總評分數請輸入數字");
				}

				Integer csscoretimes = null;
				try {
					csscoretimes = Integer.valueOf(req.getParameter("csscoretimes"));
					if (csscoretimes <= 0) {
						errorMsgs.add("評分次數請輸入大於零的整數");
					}
				} catch (NumberFormatException e) {
					csscoretimes = 0;
					errorMsgs.add("評分次數請輸入數字");
				}
				
				byte[] courseimg = null;

				CourseVO courseVO = new CourseVO();
				courseVO.setCourseno(courseno);
				courseVO.setCstypeno(cstypeno);
				courseVO.setTchrno(tchrno);
				courseVO.setCoursename(coursename);
				courseVO.setCourseinfo(courseinfo);
				courseVO.setCourseprice(courseprice);
				courseVO.setTtltime(ttltime);
				courseVO.setCsstatus(csstatus);
				courseVO.setCsscore(csscore);
				courseVO.setCsscoretimes(csscoretimes);
				courseVO.setCourseimg(courseimg);

				// Send the use back to the form, if there were errors
				if (!errorMsgs.isEmpty()) {
					req.setAttribute("courseVO", courseVO); // 含有輸入格式錯誤的empVO物件,也存入req
					RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/editCourse.jsp");
					failureView.forward(req, res);
					return;
				}
				/*************************** 2.開始新增資料 ***************************************/
				CourseService courseSvc = new CourseService();
				courseVO = courseSvc.updateCourse(courseno, cstypeno, tchrno, coursename, courseinfo, courseprice,
						ttltime, csstatus, csscore, csscoretimes, courseimg);
				courseVO = courseSvc.getOneCourse(courseno);
				/*************************** 3.新增完成,準備轉交(Send the Success view) ***********/
				req.setAttribute("courseVO", courseVO);
				String url = "/front-end/course/editCourse.jsp"; // 不用加前面的路徑？2020/09/30
				RequestDispatcher successView = req.getRequestDispatcher(url); // 新增成功後轉交listAllCourse.jsp
				successView.forward(req, res);
				/*************************** 其他可能的錯誤處理 **********************************/
			} catch (Exception e) {
				errorMsgs.add("無法更新課程:" + e.getMessage());
				RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/editCourse.jsp");
				failureView.forward(req, res);
			}
		}

		if ("insert".equals(action)) {// 新增課程

			List<String> errorMsgs = new LinkedList<String>();
			req.setAttribute("errorMsgs", errorMsgs);

			try {
				/*********************** 1.接收請求參數 - 輸入格式的錯誤處理 *************************/
				String cstypeno = req.getParameter("cstypeno");
				String tchrno = req.getParameter("tchrno");

				String coursename = req.getParameter("coursename");
				String coursenameRegex = "^[(\u4e00-\u9fa5)(a-zA-Z0-9_)]{2,10}$";
				if (coursename == null || coursename.trim().length() == 0) {
					coursename = "";
					errorMsgs.add("課程名稱請勿空白");
				} else if (!coursename.trim().matches(coursenameRegex)) {
					errorMsgs.add("課程名稱: 只能是中、英文字母、數字和_ , 且長度必需在2到10之間");
				}

				String courseinfo = req.getParameter("courseinfo");

				Integer courseprice = null;
				try {
					courseprice = Integer.valueOf(req.getParameter("courseprice"));
					if (courseprice <= 0) {
						errorMsgs.add("課程單價請輸入大於零的整數");
					}
				} catch (NumberFormatException e) {
					courseprice = 0;
					errorMsgs.add("課程單價請輸入數字");
				}

				Integer ttltime = null;
				// 應該要自己抓影片的長度
				try {
					ttltime = Integer.valueOf(req.getParameter("ttltime"));
					if (ttltime <= 0) {
						errorMsgs.add("課程總時數請輸入大於零的整數");
					}
				} catch (NumberFormatException e) {
					ttltime = 0;
					errorMsgs.add("課程總時數請輸入數字");
				}

				String csstatus = req.getParameter("csstatus");// ?

				Integer csscore = null;
				try {
					csscore = Integer.valueOf(req.getParameter("csscore"));
					if (csscore <= 0) {
						errorMsgs.add("累積總評分數請輸入大於零的整數");
					}
				} catch (NumberFormatException e) {
					csscore = 0;
					errorMsgs.add("累積總評分數請輸入數字");
				}

				// 評分次數???

				Integer csscoretimes = null;
				try {
					csscoretimes = Integer.valueOf(req.getParameter("csscoretimes"));
					if (csscoretimes <= 0) {
						errorMsgs.add("評分次數請輸入大於零的整數");
					}
				} catch (NumberFormatException e) {
					csscoretimes = 0;
					errorMsgs.add("評分次數請輸入數字");
				}

				byte[] courseimg = null;
				Part part = req.getPart("courseimg");

//				System.out.println("===== PART: " + courseimg + " =====");
				System.out.println("**SubmittedFileName = " + part.getSubmittedFileName());
				System.out.println("**ContentType = " + part.getContentType());
				System.out.println("**Size = " + part.getSize());

				if (part.getSize() == 0) {
					errorMsgs.add("請上傳課程圖片");
				} else if (part.getContentType().indexOf("image/jpeg") < 0) {
					errorMsgs.add("僅可以上傳 jpg 圖片檔案");
				} else {
					InputStream in = part.getInputStream();
					courseimg = getUpdateFileByteArray(in);
//					System.out.println(courseimg.length);
					in.close();
				}

				CourseVO courseVO = new CourseVO();
				courseVO.setCstypeno(cstypeno);
				courseVO.setTchrno(tchrno);
				courseVO.setCoursename(coursename);
				courseVO.setCourseinfo(courseinfo);
				courseVO.setCourseprice(courseprice);
				courseVO.setTtltime(ttltime);
				courseVO.setCsstatus(csstatus);
				courseVO.setCsscore(csscore);
				courseVO.setCsscoretimes(csscoretimes);
				courseVO.setCourseimg(courseimg);

				// Send the use back to the form, if there were errors
				if (!errorMsgs.isEmpty()) {
					req.setAttribute("courseVO", courseVO); // 含有輸入格式錯誤的empVO物件,也存入req
					RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/addCourse.jsp");
					failureView.forward(req, res);
					return;
				}
				/*************************** 2.開始新增資料 ***************************************/
				CourseService courseSvc = new CourseService();
				//要用自增主鍵方法再取一次，才能做到直接轉交 edit
				String courseno = courseSvc.addCourse(cstypeno, tchrno, coursename, courseinfo, courseprice, ttltime, csstatus,	csscore, csscoretimes, courseimg);
				courseVO = courseSvc.getOneCourse(courseno);
				/*************************** 3.新增完成,準備轉交(Send the Success view) ***********/
//				String url = "/front-end/course/listAllCourse.jsp";
				String url = "/front-end/course/editCourse.jsp";
				RequestDispatcher successView = req.getRequestDispatcher(url);
				successView.forward(req, res);

				/*************************** 其他可能的錯誤處理 **********************************/
			} catch (Exception e) {
				errorMsgs.add("無法新增資料:" + e.getMessage());
				RequestDispatcher failureView = req.getRequestDispatcher("/front-end/course/addCourse.jsp");
				failureView.forward(req, res);
			}
		}

	}

	public static byte[] getUpdateFileByteArray(InputStream in) throws IOException {
		byte[] buffer = new byte[in.available()];
		in.read(buffer);
		in.close();
		return buffer;
	}

}
