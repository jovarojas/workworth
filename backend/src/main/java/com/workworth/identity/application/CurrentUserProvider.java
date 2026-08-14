package com.workworth.identity.application;

import com.workworth.identity.persistence.AppUser;

public interface CurrentUserProvider {

    AppUser currentUser();
}
