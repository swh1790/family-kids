document.addEventListener('DOMContentLoaded', function () {
    // 1. 导航自动高亮：按当前路径匹配菜单项
    var path = location.pathname.replace(/\/$/, '') || '/';
    document.querySelectorAll('.navbar-app .nav-link').forEach(function (link) {
        var href = link.getAttribute('href') || '';
        href = href.replace(/\/$/, '') || '/';
        if (href === '/' || href === '') {
            if (path === '/' || path === '') link.classList.add('active');
            return;
        }
        if (path === href || path.indexOf(href + '/') === 0) {
            link.classList.add('active');
        }
    });

    // 2. 闪存消息自动关闭
    var alerts = document.querySelectorAll('.flash-msg .alert');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 3000);
    });

    // 3. 顶部提示条 4 秒后自动淡出
    var flash = document.querySelectorAll('.alert-dismissible.fade.show');
    flash.forEach(function (el) {
        setTimeout(function () {
            try { new bootstrap.Alert(el).close(); } catch (e) { /* 已关闭 */ }
        }, 4000);
    });

    // 4. Chart.js 全局主题：圆润字体与柔和提示框
    if (typeof Chart !== 'undefined') {
        Chart.defaults.font.family =
            '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif';
        Chart.defaults.font.size = 13;
        Chart.defaults.color = '#6B7688';
        Chart.defaults.plugins.tooltip.backgroundColor = 'rgba(27,33,48,0.92)';
        Chart.defaults.plugins.tooltip.cornerRadius = 12;
        Chart.defaults.plugins.tooltip.padding = 12;
        Chart.defaults.plugins.tooltip.titleFont = { weight: '700' };
        Chart.defaults.plugins.tooltip.displayColors = true;
        Chart.defaults.plugins.tooltip.usePointStyle = true;
    }
});
