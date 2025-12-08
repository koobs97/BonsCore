package com.koo.bonscore.biz.authorization.dto.res;

import lombok.*;

/**
 * <pre>
 * MenuByRoleDto.java
 * 설명 : 권한 메뉴 dto
 * </pre>
 *
 * @author  : koobonsang
 * @version : 1.0
 * @since   : 2025-08-01
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuByRoleDto {
    private String menuId;
    private String menuName;
    private String menuUrl;
    private String parentMenuId;
    private Integer sortOrder;
    private String isVisible;
}
