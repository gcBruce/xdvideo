package net.xdclass.xdvideo.service.impl;

import net.xdclass.xdvideo.config.WeChatConfig;
import net.xdclass.xdvideo.domain.User;
import net.xdclass.xdvideo.mapper.UserMapper;
import net.xdclass.xdvideo.service.UserService;
import net.xdclass.xdvideo.utils.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private WeChatConfig weChatConfig;

    @Autowired
    private UserMapper userMapper;



    @Override
    public User saveWeChatUser(String code) {

        String accessTokenUrl=String.format(WeChatConfig.getOpenAccessTokenUrl(),weChatConfig.getOpenAppid(),weChatConfig.getOpenAppsecret(),code);
        //获取access_token
        Map<String,Object> baseMap= HttpUtils.doGet(accessTokenUrl);
        if(baseMap==null||baseMap.isEmpty()){
            return null;
        }
        String accessToken=(String)baseMap.get("access_token");
        String openId=(String)baseMap.get("openid");

        //根据得到的openid判断用户是否已经注册过了
        User dbUser=userMapper.findByOpenId(openId);
        if(dbUser!=null){
            return dbUser;
        }




        //获取用户基本信息
        String userInfoUrl=String.format(WeChatConfig.getOpenUserInfoUrl(),accessToken,openId);
        Map<String,Object> baseUserMap= HttpUtils.doGet(userInfoUrl);
        if(baseUserMap==null||baseUserMap.isEmpty()){
            return null;
        }
        String nickname=(String)baseUserMap.get("nickname");
        Double sexTemp=(Double)baseUserMap.get("sex");
        int sex=sexTemp.intValue();//转成int类型
        String province=(String)baseUserMap.get("province");
        String city=(String)baseUserMap.get("city");
        String country=(String)baseUserMap.get("country");
        String headimgurl=(String)baseUserMap.get("headimgurl");
        StringBuilder sb=new StringBuilder(country).append("||").append(province).append("||").append(city);
        String finalAddress=sb.toString();

        try {
            //解决乱码
            nickname = new String(nickname.getBytes("ISO-8859-1"), "UTF-8");
            finalAddress = new String(finalAddress.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //封装User
        User user=new User();
        user.setName(nickname);
        user.setHeadImg(headimgurl);
        user.setCity(finalAddress);
        user.setSex(sex);
        user.setOpenid(openId);
        user.setCreateTime(new Date());

        //保存用户信息
        userMapper.save(user);
        return user;
    }
}
