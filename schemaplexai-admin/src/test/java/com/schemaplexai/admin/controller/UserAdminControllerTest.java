package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.UserAdminDTO;
import com.schemaplexai.admin.dto.UserAdminQuery;
import com.schemaplexai.admin.dto.UserRoleUpdateDTO;
import com.schemaplexai.admin.service.UserAdminService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

    @Mock
    private UserAdminService userAdminService;

    @InjectMocks
    private UserAdminController userAdminController;

    @Test
    void page_returnsUsers() {
        UserAdminQuery query = new UserAdminQuery();
        PageResult<UserAdminDTO> pageResult = PageResult.of(Collections.emptyList(), 0L, 1L, 10L);
        when(userAdminService.queryUsers(query)).thenReturn(pageResult);

        Result<PageResult<UserAdminDTO>> result = userAdminController.page(query);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(pageResult);
    }

    @Test
    void getDetail_returnsUser() {
        UserAdminDTO user = new UserAdminDTO();
        user.setId(1L);
        when(userAdminService.getUserDetail(1L)).thenReturn(user);

        Result<UserAdminDTO> result = userAdminController.getDetail(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(user);
    }

    @Test
    void disable_returnsSuccess() {
        Result<Void> result = userAdminController.disable(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(userAdminService).disableUser(1L);
    }

    @Test
    void enable_returnsSuccess() {
        Result<Void> result = userAdminController.enable(1L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(userAdminService).enableUser(1L);
    }

    @Test
    void resetPassword_returnsTemporaryPassword() {
        when(userAdminService.resetUserPassword(1L)).thenReturn("tempPass123");

        Result<Map<String, String>> result = userAdminController.resetPassword(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().get("temporaryPassword")).isEqualTo("tempPass123");
    }

    @Test
    void getRoles_returnsRoleNames() {
        when(userAdminService.getUserRoles(1L)).thenReturn(List.of("ADMIN", "USER"));

        Result<List<String>> result = userAdminController.getRoles(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsExactly("ADMIN", "USER");
    }

    @Test
    void updateRoles_returnsSuccess() {
        UserRoleUpdateDTO dto = new UserRoleUpdateDTO();
        dto.setRoleIds(List.of(1L, 2L));

        Result<Void> result = userAdminController.updateRoles(1L, dto);

        assertThat(result.getCode()).isEqualTo(200);
        verify(userAdminService).updateUserRoles(1L, dto);
    }
}
