import { defineStore } from 'pinia';

export const userStore = defineStore('user', {
    state: () => ({
        userInfo: {
            userId      : null,
            userName    : null,
            email       : null,
            phoneNumber : null,
            birthDate   : null,
            genderCode  : null,
        }
    }),
    getters: {
        isAuthenticated: (state) => {
            return !!state.userInfo.userId;
        },
        isLoggedIn: (state) => {
            return state.userInfo.userId !== null
        },
        getUserInfo: () => {
            return this.userInfo;
        }
    },
    actions: {
        setUser(user = {}) {
            this.userInfo = user || {
                userId      : null,
                userName    : null,
                email       : null,
                phoneNumber : null,
                birthDate   : null,
                genderCode  : null,
            };
        },
        cleanUser() {
            this.setUser();
        },
    },
});