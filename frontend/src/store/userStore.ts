import { defineStore } from 'pinia';

export interface userState {
    userId      : string,
    userName    : string,
    email       : string,
    phoneNumber : string,
    birthDate   : string,
    genderCode  : string,
    loginTime   : string,
}

export const userStore = defineStore('user', {
    state: () => ({
        userInfo: {
            userId      : '' as string,
            userName    : '' as string,
            email       : '' as string,
            phoneNumber : '' as string,
            birthDate   : '' as string,
            genderCode  : '' as string,
            loginTime   : '' as string,
        } as userState
    }),
    getters: {
        isAuthenticated(): boolean {
            return this.userInfo.userId != ''
        },
        isLoggedIn: (state) => {
            return state.userInfo.userId !== ''
        },
        getUserInfo: (state) => {
            return state.userInfo
        },
    },
    actions: {
        /* 유저 정보 세팅 */
        setUserInfo(userInfo: userState): void {
            this.userInfo = userInfo
        },
        delUserInfo() {
            Object.keys(this.userInfo).forEach(key => {
                this.userInfo[key as keyof userState] = '';
            });
        }
    },
});