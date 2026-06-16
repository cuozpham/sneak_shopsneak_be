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

    @GetMapping("/privacy")
    public ApiResponse<String> getPrivacyPolicy() {
        String privacy = """
                CHÍNH SÁCH BẢO MẬT & QUYỀN RIÊNG TƯ
                
                Cảm ơn quý khách đã truy cập vào website được vận hành bởi CÔNG TY TNHH MANDRO.
                
                Chúng tôi tôn trọng và cam kết bảo mật những thông tin mang tính riêng tư của quý khách. Xin vui lòng đọc kỹ bản chính sách bảo mật dưới đây để hiểu rõ hơn những cam kết mà chúng tôi thực hiện nhằm tôn trọng và bảo vệ quyền lợi của người truy cập.
                
                1. Thu thập và sử dụng thông tin
                MANDRO chỉ thu thập các thông tin cơ bản liên quan đến đơn đặt hàng bao gồm: Họ tên, số điện thoại, và địa chỉ giao hàng.
                
                Mục đích sử dụng: Xử lý đơn hàng, nghiên cứu thị trường và nâng cao chất lượng dịch vụ chăm sóc khách hàng.
                
                Cam kết từ MANDRO:
                - Mọi thông tin cá nhân của quý khách chỉ được sử dụng đúng mục đích đã thông báo.
                - Việc thu thập thông tin đều được thực hiện dựa trên sự đồng ý và tự nguyện của khách hàng.
                - Thông tin khách hàng được bảo mật nội bộ và chỉ những bộ phận có thẩm quyền mới được phép tiếp cận (bao gồm: Bộ phận chăm sóc khách hàng, bộ phận Marketing, và nhân viên bán hàng).
                
                Lưu ý: Trong quá trình nhận thông tin quảng cáo hoặc chăm sóc khách hàng, quý khách hoàn toàn có quyền yêu cầu MANDRO dừng sử dụng thông tin của mình bất kỳ lúc nào.
                
                2. Biện pháp bảo mật thông tin
                Trách nhiệm nội bộ: Mỗi nhân viên tại MANDRO đều được quán triệt và có trách nhiệm tuyệt đối trong việc tuân thủ các quy định bảo mật thông tin của khách hàng.
                
                Xử lý sự cố: Trong trường hợp máy chủ lưu trữ thông tin bị hacker tấn công dẫn đến nguy cơ mất mát dữ liệu của khách hàng, MANDRO sẽ nhanh chóng thông báo vụ việc cho các cơ quan chức năng có thẩm quyền để kịp thời điều tra và xử lý.
                
                3. Trách nhiệm đảm bảo quyền riêng tư từ phía khách hàng
                Để bảo vệ quyền lợi của chính mình, MANDRO khuyến nghị quý khách lưu ý các điểm sau:
                - Cung cấp thông tin: Quý khách chỉ cần cung cấp đầy đủ và chính xác các thông tin cơ bản theo yêu cầu của MANDRO. Quý khách phải tự chịu trách nhiệm về tính trung thực của các thông tin này, cũng như tự chịu rủi ro nếu tự ý cung cấp các thông tin ngoài phạm vi yêu cầu.
                - Bảo mật tài khoản: Tuyệt đối không cung cấp các thông tin liên quan đến tài khoản ngân hàng (khi chưa được mã hóa trong các giao dịch thanh toán trực tuyến) hoặc các thông tin cá nhân nhạy cảm khác.
                - Trường hợp chia sẻ thông tin: Nếu quý khách cung cấp thông tin cá nhân cho nhiều tổ chức, cá nhân khác nhau, quý khách cần yêu cầu các bên đó cùng có trách nhiệm bảo mật.
                - Miễn trừ trách nhiệm: Trong trường hợp thông tin của quý khách bị tiết lộ gây thiệt hại, quý khách cần chủ động xác định chính xác nguồn lộ thông tin. MANDRO sẽ không chịu trách nhiệm đối với các sự cố rò rỉ dữ liệu nếu không có căn cứ xác đáng chứng minh MANDRO là bên vi phạm cam kết bảo mật.
                
                Trên đây là toàn bộ chính sách bảo mật của MANDRO. Nếu quý khách còn bất kỳ thắc mắc hay câu hỏi nào, xin vui lòng liên hệ trực tiếp với chúng tôi để được hỗ trợ và giải đáp kịp thời!
                """;
        return ApiResponse.ok("success", privacy);
    }

    @GetMapping("/exchange")
    public ApiResponse<String> getExchangePolicy() {
        String exchange = """
                CHÍNH SÁCH ĐỔI HÀNG
                
                Khách hàng được đổi sản phẩm trong vòng 15 ngày kể từ ngày mua hàng trong trường hợp sản phẩm không đúng với mô tả trên website, bị lỗi kỹ thuật, không vừa size hoặc không còn ưng ý đối với sản phẩm đã đặt trong điều kiện:
                
                - Quý khách còn hoá đơn mua hàng và thông tin khách hàng trên hoá đơn (Bao gồm tên và số điện thoại)
                
                - Sản phẩm chưa qua sử dụng, không bị hư hỏng do hoá chất hoặc tác động ngoại lực, lỗi từ người dùng (va quệt, co kéo, bảo quản không đúng cách, tự ý thay đổi kết cấu và màu sắc của sản phẩm…)
                
                - Sản phẩm đổi mới có giá trị bằng hoặc cao hơn sản phẩm đã mua. Trong trường hợp sản phẩm được chọn để đổi có giá trị cao hơn, khách hàng vui lòng thanh toán thêm phần chênh lệch. Nếu khách hàng lựa chọn sản phẩm được chọn để đổi có giá trị thấp hơn, MANDRO sẽ không hoàn lại tiền thừa.
                
                Cách thức đổi sản phẩm
                
                1. Đối với khách hàng mua tại cửa hàng
                Khách hàng có thể gửi trực tiếp sản phẩm cần đổi trả kèm theo hoá đơn mua hàng đến các showroom của MANDRO trên toàn quốc.
                
                2. Đối với khách hàng mua online
                Khách hàng liên hệ với fanpage hoặc hotline để nhân viên tư vấn, hướng dẫn gửi sản phẩm về showroom gần nhất.
                
                Sau khi đã nhận được sản phẩm trả lại, chúng tôi sẽ tiến hành kiểm tra sản phẩm theo quy định đổi trả (thời gian từ 3 - 7 ngày không tính ngày Lễ, Tết) để xử lý và thông báo đến khách hàng qua điện thoại.
                """;
        return ApiResponse.ok("success", exchange);
    }

    @GetMapping("/warranty")
    public ApiResponse<String> getWarrantyPolicy() {
        String warranty = """
                <h1 style="font-size:2.2rem;font-weight:800;letter-spacing:0.04em;margin-bottom:0.25em;">CHÍNH SÁCH BẢO HÀNH</h1>
                <hr style="margin:1rem 0 1.5rem 0;border:none;border-top:2px solid #e5e7eb;" />
                <p style="margin-bottom:0.5em;">MANDRO CAM KẾT VỚI QUÝ KHÁCH HÀNG VỀ CHÍNH SÁCH BẢO HÀNH VÀ BẢO TRÌ SẢN PHẨM NHƯ SAU:</p>
                <p style="margin-bottom:1.5em;">Bảo hành miễn phí 6 tháng kể từ ngày mua hàng được ghi trên hoá đơn. <em>(Lưu ý: Không áp dụng với hàng sale từ 50% trở lên)</em></p>
                <h2 style="font-size:1.1rem;font-weight:700;margin-top:1.75em;margin-bottom:0.75em;">QUY ĐỊNH CHUNG VỀ BẢO HÀNH</h2>
                <p style="margin-bottom:0.5em;">Khắc phục những lỗi hỏng do nhà sản xuất như: nở da, bong tróc, bung đế...</p>
                <p style="margin-bottom:0.5em;">Kênh tiếp nhận bảo hành chính thức: <a href="https://zalo.me/0934762018" target="_blank" rel="noopener noreferrer" style="color:#2563eb;font-weight:600;text-decoration:underline;">OA ZALO MANDRO</a>.</p>
                <p style="margin-bottom:1.5em;">Mỗi số điện thoại khách hàng đăng ký tương ứng một mã bảo hành hợp lệ.</p>
                <h2 style="font-size:1.1rem;font-weight:700;margin-top:1.75em;margin-bottom:0.75em;">NHỮNG TRƯỜNG HỢP KHÔNG ĐƯỢC BẢO HÀNH</h2>
                <p style="margin-bottom:0.5em;">Không nhận các sản phẩm quá cũ (đế và da bị lão hoá, không có độ bám dính của keo hoặc không có phụ kiện để thay).</p>
                <p style="margin-bottom:1.5em;">Những sản phẩm bị hư, hỏng do hoả hoạn, tác động ngoại lực hoặc do lỗi từ người dùng (va quệt, cào kéo, bảo quản không đúng cách, tự ý thay đổi kết cấu, màu sắc của sản phẩm...).</p>
                <h2 style="font-size:1.1rem;font-weight:700;margin-top:1.75em;margin-bottom:0.75em;">QUY TRÌNH BẢO HÀNH</h2>
                <p style="margin-bottom:0.5em;">Liên hệ cho kênh <a href="https://zalo.me/0934762018" target="_blank" rel="noopener noreferrer" style="color:#2563eb;font-weight:600;text-decoration:underline;">OA ZALO MANDRO</a>.</p>
                <p style="margin-bottom:0.5em;">Nếu lỗi nằm trong hạng mục bảo hành, khách hàng sẽ gửi sản phẩm về Trung tâm bảo hành theo hướng dẫn của nhân viên CSKH.</p>
                <p style="margin-bottom:0.5em;">Sau khi nhận được sản phẩm cần bảo hành, Bộ phận Bảo hành của MANDRO sẽ tiến hành kiểm tra, đánh giá và xác định nguyên nhân lỗi. Sau đó, thông báo cho khách hàng về hạng mục bảo hành và dự trù chi phí sửa chữa (nếu có).</p>
                <p style="margin-bottom:0.5em;">Đối với những sản phẩm đã qua sử dụng, MANDRO sẽ tiến hành bảo hành trong vòng từ 7 - 10 ngày làm việc (không bao gồm các ngày nghỉ lễ, tết).</p>
                <p style="margin-bottom:1.5em;">Sau khi đã sửa chữa xong, MANDRO sẽ liên hệ lại với quý khách, đặt lịch hẹn trả sản phẩm và thông báo về chi phí sửa chữa (nếu có).</p>
                <hr style="margin:1.5rem 0;border:none;border-top:1px solid #e5e7eb;" />
                <p>Hotline: <a href="tel:0934762018" style="color:#2563eb;font-weight:600;">0934762018</a></p>
                """;
        return ApiResponse.ok("success", warranty);
    }
}
