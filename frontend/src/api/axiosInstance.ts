import axios from 'axios';
import { ApiUrls } from './apiUrls';
import { ElLoading } from 'element-plus';

export class Api {

    /**
     * Axios Transaction post
     * @param url           // backend url
     * @param params        // 보낼 파라미터
     * @param loadingOption // loading 옵션 사용 여부
     * @returns
     */
    public static post = async (url: ApiUrls, params: Object, loadingOption?: Boolean): Promise<any> => {

        if(loadingOption) {
            const loading = ElLoading.service({
                lock: true,
                text: 'Loading',
                background: 'rgba(0, 0, 0, 0.7)',
            })

            try {

                const retData = await axios.post(url, params)

                loading.close()

                return retData
            } catch (error) {
                loading.close()
                return Promise.reject(error)
            }
        }
        else {

            try {
                return await axios.post(url, params)
            } catch (error) {
                return Promise.reject(error)
            }
        }
    }

}
