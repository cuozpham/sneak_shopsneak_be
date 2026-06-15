package sneak_shop.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sneak_shop.common.response.ApiResponse;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    @GetMapping("/terms")
    public ApiResponse<String> getTermsOfService() {
        String terms = """
                ĐIỀU KHOẢN DỊCH VỤ
                
                Xin vui lòng đọc kỹ phần này vì nó có các điều khoản và các điều kiện pháp lý mà quý khách phải chấp thuận khi sử dụng trang web này.
                
                Người điều hành
                Website này được điều hành bởi MANDRO (mandro.vn).
                
                Mục đích của trang web
                Mục đích của trang này nhằm cung cấp cho quý khách các thông tin về sản phẩm, cửa hàng, những chiến dịch mà chúng tôi ủng hộ và những hoạt động chúng tôi thực hiện.
                
                Khả năng truy cập vào trang web
                Chúng tôi đã chuẩn bị rất kỹ lưỡng cho trang web này. Tuy nhiên vì những lý do kỹ thuật nằm ngoài sự kiểm soát, chúng tôi sẽ không đảm bảo rằng bạn có thể truy cập được vào trang này bất cứ lúc nào.
                
                Thông tin tại website
                Thông tin trên trang này chỉ nằm mục đích thông tin thông thường. Nó không nhằm vào bất cứ mục đích cụ thể nào và cũng không có sự đại diện hoặc bảo đảm nào cho sự chính xác và đầy đủ.
                
                Sản phẩm và dịch vụ sẵn có
                Không phải bất kỳ sản phẩm hay dịch vụ nào được tham khảo trên trang này đều được cung cấp và bán và cũng không có nghĩa là sản phẩm hay dịch vụ luôn có sẵn ở tất cả các nước hoặc tên và thông tin về sản phẩm trong cửa hàng ở địa phương giống như những gì được nói trong trang này.
                
                Liên kết ngoài
                Chúng tôi cung cấp sự liên kết với những trang web khác mọi lúc. Những liên kết này được cung cấp nhằm mang đến sự thuận tiện cho bạn. Chúng tôi không chứng thực và không chịu trách nhiệm đối với nội dung trong những trang web này, khả năng truy cập vào những trang web này và sẽ không chịu bất kỳ trách nhiệm nào cho sự tổn thất và mất mát của bạn khi truy cập vào những trang web này. Bạn phải chịu tất cả những rủi ro nếu bạn quyết định truy cập vào những trang web này.
                
                Từ chối bảo đảm và trách nhiệm
                Đối với phạm vi đầy đủ nhất được cho phép bởi luật, tất cả sự bảo đảm (biểu hiện hoặc ẩn ý) đối với trang này, nội dung và việc sử dụng của bạn sẽ bị loại trừ. Ngoại trừ trường hợp tử vong hoặc thương tích cá nhân gây ra bởi sự sơ suất của chúng tôi, luật pháp cho phép chúng tôi loại trừ tất cả trách nhiệm với bạn trong việc bạn sử dụng website.
                
                Sự sửa đổi những điều khoản
                Chúng tôi bảo lưu quyền sửa đổi các điều khoản này mà không cần thông báo với bạn mọi lúc. Việc sửa đổi sẽ có hiệu lực mỗi khi các điều khoản chỉnh sửa đã được đăng trên trang web.
                
                Luật áp dụng
                Các Điều khoản này bị chi phối và sẽ được hiểu theo quy định của pháp luật Việt Nam. Mọi tranh chấp phát sinh hoặc liên quan đến các Điều khoản và Điều kiện này sẽ chịu sự xét xử của Tòa án Việt Nam.
                """;
        return ApiResponse.ok("success", terms);
    }
}
