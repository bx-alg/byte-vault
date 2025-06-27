package com.bytevault.app.controller;

import com.bytevault.app.model.Role;
import com.bytevault.app.model.User;
import com.bytevault.app.service.RoleService;
import com.bytevault.app.service.UserService;
import com.bytevault.app.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('admin')")  // 类级别权限控制，只有admin角色可以访问
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;
    private final TokenService tokenService;

    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * 获取所有角色
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    /**
     * 创建角色
     */
    @PostMapping("/roles")
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        return ResponseEntity.ok(roleService.createRole(role));
    }

    /**
     * 更新角色
     */
    @PutMapping("/roles/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return ResponseEntity.ok(roleService.updateRole(role));
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 为角色分配权限
     */
    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<Void> assignPermissionsToRole(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds) {
        roleService.assignPermissionsToRole(roleId, permissionIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Void> assignRolesToUser(
            @PathVariable Long userId,
            @RequestBody List<Long> roleIds) {
        roleService.assignRolesToUser(userId, roleIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 封禁用户
     *
     * @param userId  用户ID
     * @param minutes 封禁时间（分钟）
     * @param reason  封禁原因
     * @return 封禁结果
     */
    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<?> banUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "60") int minutes,
            @RequestParam(required = false, defaultValue = "违反用户协议") String reason) {

        try {
            // 检查用户是否存在
            if (!userService.existsById(userId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
            }

            // 封禁用户
            tokenService.banUser(userId, reason, minutes);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户已被封禁");
            response.put("userId", userId);
            response.put("banTime", minutes);
            response.put("reason", reason);

            log.info("用户 {} 已被封禁 {} 分钟，原因: {}", userId, minutes, reason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("封禁用户失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "封禁用户失败: " + e.getMessage()));
        }
    }

    /**
     * 解除用户封禁
     *
     * @param userId 用户ID
     * @return 解除结果
     */
    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long userId) {
        try {
            // 检查用户是否存在
            if (!userService.existsById(userId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
            }

            // 检查用户是否被封禁
            String banInfo = tokenService.getUserBanInfo(userId);
            if (banInfo == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户未被封禁"));
            }

            // 解除封禁（设置过期时间为0，立即过期）
            tokenService.banUser(userId, null, 0);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "用户封禁已解除");
            response.put("userId", userId);

            log.info("用户 {} 的封禁已解除", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("解除用户封禁失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "解除用户封禁失败: " + e.getMessage()));
        }
    }

    /**
     * 查询用户封禁状态
     *
     * @param userId 用户ID
     * @return 封禁信息
     */
    @GetMapping("/users/{userId}/ban-status")
    public ResponseEntity<?> getUserBanStatus(@PathVariable Long userId) {
        try {
            // 检查用户是否存在
            if (!userService.existsById(userId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "用户不存在"));
            }

            // 获取封禁信息
            String banInfo = tokenService.getUserBanInfo(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("banned", banInfo != null);
            if (banInfo != null) {
                response.put("reason", banInfo);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户封禁状态失败: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "获取用户封禁状态失败: " + e.getMessage()));
        }
    }
} 