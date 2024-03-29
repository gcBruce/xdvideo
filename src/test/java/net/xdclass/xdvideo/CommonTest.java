package net.xdclass.xdvideo;

import io.jsonwebtoken.Claims;
import net.xdclass.xdvideo.domain.User;
import net.xdclass.xdvideo.utils.JwtUtils;
import org.junit.Test;

public class CommonTest {

    @Test
    public void testGeneJwt(){
        User user =new User();
        user.setId(999);
        user.setHeadImg("www.xdclass.net");
        user.setName("xd");

        String token=JwtUtils.geneJsonWebToken(user);
        System.out.println(token);

    }
    @Test
    public void testCheck(){
        String token="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4ZGNsYXNzIiwiaWQiOjk5OSwibmFtZSI6InhkIiwiaW1nIjoid3d3LnhkY2xhc3MubmV0IiwiaWF0IjoxNTU4Nzc1NzMwLCJleHAiOjE1NTkzODA1MzB9.6s8CJFf62YjC_gMnFoLFA3UVzVOITILE0hxsiFZHkBc";
        Claims claims=JwtUtils.checkJWT(token);
        if(claims!=null){
            String name=(String) claims.get("name");
            String img=(String) claims.get("img");
            int id=(Integer) claims.get("id");
            System.out.println(name);
            System.out.println(img);
            System.out.println(id);
        }else{
            System.out.println("非法token");
        }
    }
}
