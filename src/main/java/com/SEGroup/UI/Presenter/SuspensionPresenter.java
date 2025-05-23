package com.SEGroup.UI.Presenter;

import com.SEGroup.Service.Result;
import com.SEGroup.Service.UserService;
import com.SEGroup.UI.SecurityContextHolder;
import com.SEGroup.UI.ServiceLocator;
import com.SEGroup.UI.Views.SuspensionView;

import java.util.List;
import java.util.stream.Collectors;

public class SuspensionPresenter {
    private final SuspensionView view;
    private final UserService userService;

    public SuspensionPresenter(SuspensionView view) {
        this.view = view;
        this.userService = ServiceLocator.getUserService();
    }

    public List<String> getAllUserEmails() {
        return userService.allUsersEmails();
    }

    public void suspendUser(String email, int days, String reason) {
        //put session key and reason !!
        Result<?> result = userService.suspendUser(SecurityContextHolder.token(),email, days, reason);
        handleResult(result);
    }

    public void unsuspendUser(String email) {
        //put session key !!!
        Result<?> result = userService.unsuspendUser(SecurityContextHolder.token(),email);
        handleResult(result);
    }

    public void loadSuspensions() {
        var suspensions = userService.allSuspensions()
                .stream()
                .map(dto -> new SuspensionView.Susp(dto.getUserEmail(),(dto.getStartTime()).toString(), (dto.getEndTime()).toString()))
                .collect(Collectors.toList());
        view.showSuspensions(suspensions);
    }

    private void handleResult(Result<?> result) {
        String message = result.isSuccess() ? "OK" : result.getErrorMessage();
        view.showMessage(message);
        loadSuspensions();
    }
}