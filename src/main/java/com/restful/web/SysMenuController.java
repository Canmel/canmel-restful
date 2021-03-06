package com.restful.web;


import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.core.entity.ErrorResult;
import com.core.entity.HttpResult;
import com.core.entity.Result;
import com.restful.annotation.SaveLog;
import com.restful.entity.SysMenu;
import com.restful.entity.enums.MenuLevel;
import com.restful.service.SysMenuService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author  *
 *   ┏ ┓   ┏ ┓
 *  ┏┛ ┻━━━┛ ┻┓
 *  ┃         ┃
 *  ┃    ━    ┃
 *  ┃  ┳┛  ┗┳ ┃
 *  ┃         ┃
 *  ┃    ┻    ┃
 *  ┃         ┃
 *  ┗━━┓    ┏━┛
 *     ┃    ┃神兽保佑
 *     ┃    ┃代码无BUG！
 *     ┃    ┗━━━━━━━┓
 *     ┃            ┣┓
 *     ┃            ┏┛
 *     ┗┓┓┏━━━━━━┳┓┏┛
 *      ┃┫┫      ┃┫┫
 *      ┗┻┛      ┗┻┛
 * @since 2018-08-15
 */
@RestController
@RequestMapping("/api/menus")
@Api(value = "菜单接口", description = "菜单接口")
public class SysMenuController extends BaseController {

    @Autowired
    private SysMenuService sysMenuService;

    /**
     * param:
     *   menu: 查询项
     * describe: 分页查询菜单信息
     * creat_user: baily
     * creat_date: 2018/8/17
     **/
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @ApiOperation(value = "分页查询菜单信息",notes = "分页查询菜单信息",httpMethod = "GET",produces = APPLICATION_JSON_UTF8_VALUE)
    public HttpResult index(SysMenu menu) {
        EntityWrapper<SysMenu> entityWrapper = new EntityWrapper<SysMenu>();
        entityWrapper.like("menuname", menu.getMenuname()).like("description", menu.getDescription());
        Page<SysMenu> menuPage = new Page<SysMenu>(menu.getCurrentPage(), 10);
        return Result.OK(sysMenuService.selectPage(menuPage, entityWrapper));
    }

    /**
     * describe: 查看菜单详细信息
     * creat_user: baily
     * creat_date: 2018/8/17
     **/
    @GetMapping("/{id}")
    @ApiOperation(value = "查看菜单详细信息", notes = "查看菜单详细信息")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public HttpResult details(HttpServletRequest request, @PathVariable Integer id) {
        return Result.OK(sysMenuService.selectById(id));
    }

    /**
     * describe: 修改菜单
     * creat_user: baily
     * creat_date: 2018/8/17
     **/
    @PutMapping("/{id}")
    @SaveLog(title = "修改菜单", value = "修改菜单")
    public HttpResult update(HttpServletRequest request, HttpServletResponse response, @RequestBody SysMenu menu, @PathVariable Integer id) {
        if (sysMenuService.updateById(menu.addId(id))) {
            return Result.OK("修改菜单成功!");
        } else {
            return ErrorResult.EXPECTATION_FAILED();
        }
    }

    /**
     * describe: 新建菜单
     * creat_user: baily
     * creat_date: 2018/8/17
     **/
    @PostMapping()
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @SaveLog(title = "新建菜单", value = "新建菜单")
    public HttpResult create(@RequestBody SysMenu menu) {
        if (sysMenuService.insert(menu)) {
            return Result.OK("新建菜单成功");
        } else {
            return ErrorResult.EXPECTATION_FAILED();
        }
    }

    /**
     * describe: 根据id删除菜单
     * creat_user: baily
     * creat_date: 2018/8/17
     **/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public HttpResult delete(@PathVariable Integer id) {
        if (sysMenuService.deleteById(id)) {
            return Result.OK("删除菜单成功!");
        } else {
            return ErrorResult.EXPECTATION_FAILED();
        }
    }

    /**
     * describe: 查询所有一级菜单
     * creat_user: baily
     * creat_date: 2018/8/17
     **/
    @GetMapping("/tops")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public HttpResult top(HttpServletRequest request) {
        EntityWrapper<SysMenu> menuEntityWrapper = new EntityWrapper<SysMenu>();
        menuEntityWrapper.eq("level", MenuLevel.TOP_MENU.getValue());
        return Result.OK(sysMenuService.selectList(menuEntityWrapper));
    }

    /**
     * describe: 查询所有二级菜单
     * creat_user: baily
     * creat_date: 2018/8/17
     **/
    @GetMapping("/subs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public HttpResult subs(HttpServletRequest request) {
        EntityWrapper<SysMenu> menuEntityWrapper = new EntityWrapper<SysMenu>();
        menuEntityWrapper.eq("level", MenuLevel.SUB_MENU.getValue());
        return Result.OK(sysMenuService.selectList(menuEntityWrapper));
    }

    @ApiOperation(value = "验证菜单名称是否可用", notes = "验证菜单名称是否可用")
    @PostMapping("/name/valid")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public HttpResult validMenuNameUsed(@RequestBody SysMenu sysMenu) {
        Map<String, Object> map = new HashMap<>();
        map.put("menuname", sysMenu.getMenuname());
        List<SysMenu> menus = sysMenuService.selectByMap(map);
        if (menus.size() > 1) {
            return ErrorResult.EXPECTATION_FAILED("菜单名已存在");
        }
        if (menus.size() == 0) {
            return Result.OK("菜单名可用");
        }
        SysMenu menu = menus.get(0);
        if (!ObjectUtils.isEmpty(sysMenu.getId()) && menu.getId().equals(sysMenu.getId())) {
            return Result.OK("菜单名可用");
        } else {
            return ErrorResult.EXPECTATION_FAILED("菜单名已存在");
        }
    }
}

