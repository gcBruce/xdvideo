package net.xdclass.xdvideo.controller;

import net.xdclass.xdvideo.config.WeChatConfig;
import net.xdclass.xdvideo.domain.JsonData;
import net.xdclass.xdvideo.domain.User;
import net.xdclass.xdvideo.domain.VideoOrder;
import net.xdclass.xdvideo.service.UserService;
import net.xdclass.xdvideo.service.VideoOrderService;
import net.xdclass.xdvideo.utils.JwtUtils;
import net.xdclass.xdvideo.utils.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;

@Controller
@RequestMapping("/api/v1/wechat")
public class WechatController {

    @Autowired
    private WeChatConfig weChatConfig;

    @Autowired
    private UserService userService;

    @Autowired
    private VideoOrderService videoOrderService;

    /**
     * 拼装微信扫一扫登陆url
     * @return
     */
    @GetMapping("login_url")
    @ResponseBody
    public JsonData loginUrl(@RequestParam(value = "access_page",required = true)String accessPage) throws UnsupportedEncodingException {
        String redirectUrl=weChatConfig.getOpenRedirectUrl();//获取开放平台重定向地址
        String callbackUrl=URLEncoder.encode(redirectUrl,"GBK");//进行编码

        String qrcodeUrl=String.format(weChatConfig.getOpenQrcodeUrl(),weChatConfig.getOpenAppid(),callbackUrl,accessPage);

        return JsonData.buildSuccess(qrcodeUrl);
    }


    /**
     * 微信扫码登陆回调地址
     * @param code
     * @param state
     * @param response
     * @throws IOException
     */
    @GetMapping("/user/callback")
    public void wechatUserCallback(@RequestParam(value = "code",required = true) String code, String state, HttpServletResponse response) throws IOException {
        //校验code,防止恶意刷接口


        User user=userService.saveWeChatUser(code);
        if(user!=null){
            //生成jwt
            String token=JwtUtils.geneJsonWebToken(user);
            //state当前用户的页面地址，需要拼接 http:// 这样才不会站内跳转
            response.sendRedirect(state+"?token="+token+"&head_img="+user.getHeadImg()+"&name"+URLEncoder.encode(user.getName(),"UTF-8"));

        }
    }


    /**
     * 微信支付回调
     */
    @PostMapping("/order/callback")
    public void orderCallback(HttpServletRequest request,HttpServletResponse response) throws Exception {
        //用下面的方法获取请求的数据
        InputStream inputStream=request.getInputStream();
        //BufferedReader是包装涉及模式，性能更高
        BufferedReader in=new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
        StringBuffer sb=new StringBuffer();
        String line;
        while ((line=in.readLine())!=null){
            sb.append(line);
        }
        in.close();
        inputStream.close();
        Map<String,String> callbackMap=WXPayUtil.xmlToMap(sb.toString());
        //将map转成sortedmap
        SortedMap<String, String> sortedMap = WXPayUtil.getSortedMap(callbackMap);
        //判断签名是否正确
        if(WXPayUtil.isCorrectSign(sortedMap,weChatConfig.getKey())){
            if("SUCCESS".equals(sortedMap.get("result_code"))){
                String outTradeNo=sortedMap.get("out_trde_no");
                VideoOrder dbvideoOrder = videoOrderService.findByOutTradeNo(outTradeNo);
                if(dbvideoOrder!=null&&dbvideoOrder.getState()==0){//判断逻辑看业务场景
                    //更新订单状态
                    VideoOrder videoOrder=new VideoOrder();
                    videoOrder.setOpenid(sortedMap.get("openid"));
                    videoOrder.setOutTradeNo(outTradeNo);
                    videoOrder.setNotifyTime(new Date());
                    videoOrder.setState(1);
                    int rows=videoOrderService.updateVideoOrderByOutTradeNo(videoOrder);
                    if(rows==1){//通知微信订单处理成功
                        //影响行数row==1响应微信成功
                        response.setContentType("text/xml");
                        response.getWriter().println("success");
                        return;
                    }
                }
            }
        }
        //影响行数row==0响应微信失败
        //失败可以不用响应
        response.setContentType("text/xml");
        response.getWriter().println("fail");
    }
}
