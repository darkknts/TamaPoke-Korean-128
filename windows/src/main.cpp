#define UNICODE
#define _UNICODE
#include <windows.h>

namespace {
constexpr wchar_t kClassName[] = L"TamaPokeDesktopWindow";
constexpr int kWindowWidth = 520;
constexpr int kWindowHeight = 650;

void CenterText(HDC dc, const wchar_t* value, RECT area, int size, COLORREF color, bool bold = false) {
  HFONT font = CreateFontW(size, 0, 0, 0, bold ? FW_BOLD : FW_NORMAL, FALSE, FALSE, FALSE,
    DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
    DEFAULT_PITCH, L"Malgun Gothic");
  HGDIOBJ old = SelectObject(dc, font);
  SetBkMode(dc, TRANSPARENT);
  SetTextColor(dc, color);
  DrawTextW(dc, value, -1, &area, DT_CENTER | DT_VCENTER | DT_SINGLELINE);
  SelectObject(dc, old);
  DeleteObject(font);
}
void Button(HDC dc, RECT area, const wchar_t* label) {
  HBRUSH brush = CreateSolidBrush(RGB(255, 244, 224));
  HPEN pen = CreatePen(PS_SOLID, 2, RGB(196, 145, 104));
  HGDIOBJ ob = SelectObject(dc, brush), op = SelectObject(dc, pen);
  RoundRect(dc, area.left, area.top, area.right, area.bottom, 14, 14);
  SelectObject(dc, ob); SelectObject(dc, op);
  DeleteObject(brush); DeleteObject(pen);
  CenterText(dc, label, area, 18, RGB(91, 65, 52), true);
}
void Paint(HWND hwnd) {
  PAINTSTRUCT ps{};
  HDC dc = BeginPaint(hwnd, &ps);
  RECT client{}; GetClientRect(hwnd, &client);
  HBRUSH bg = CreateSolidBrush(RGB(248, 241, 229));
  FillRect(dc, &client, bg); DeleteObject(bg);

  CenterText(dc, L"TamaPoke", RECT{20, 14, 500, 54}, 30, RGB(83, 66, 54), true);

  HBRUSH dark = CreateSolidBrush(RGB(38, 43, 55));
  HPEN rim = CreatePen(PS_SOLID, 7, RGB(205, 164, 112));
  HGDIOBJ ob = SelectObject(dc, dark), op = SelectObject(dc, rim);
  Ellipse(dc, 110, 76, 410, 376);
  SelectObject(dc, ob); SelectObject(dc, op); DeleteObject(dark); DeleteObject(rim);

  CenterText(dc, L"Windows edition", RECT{130, 170, 390, 220}, 22, RGB(255, 230, 155), true);
  CenterText(dc, L"게임 화면 연결 작업 중", RECT{130, 245, 390, 300}, 17, RGB(224, 224, 224), true);
  CenterText(dc, L"배고픔     행복     에너지     청결", RECT{35, 395, 485, 435}, 17, RGB(83, 66, 54), true);
  CenterText(dc, L"현재는 UI 미리보기 단계입니다", RECT{35, 438, 485, 470}, 14, RGB(125, 104, 87));
  Button(dc, RECT{28, 500, 138, 555}, L"먹이");
  Button(dc, RECT{148, 500, 258, 555}, L"놀기");
  Button(dc, RECT{268, 500, 378, 555}, L"훈련");
  Button(dc, RECT{388, 500, 498, 555}, L"도감");
  EndPaint(hwnd, &ps);
}
LRESULT CALLBACK WindowProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
  switch (msg) {
    case WM_PAINT: Paint(hwnd); return 0;
    case WM_ERASEBKGND: return 1;
    case WM_DESTROY: PostQuitMessage(0); return 0;
    default: return DefWindowProcW(hwnd, msg, wp, lp);
  }
}
}
int WINAPI wWinMain(HINSTANCE instance, HINSTANCE, PWSTR, int show) {
  WNDCLASSW wc{}; wc.lpfnWndProc = WindowProc; wc.hInstance = instance;
  wc.lpszClassName = kClassName; wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
  wc.hbrBackground = reinterpret_cast<HBRUSH>(COLOR_WINDOW + 1);
  if (!RegisterClassW(&wc)) return 1;
  RECT r{0, 0, kWindowWidth, kWindowHeight};
  AdjustWindowRect(&r, WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX, FALSE);
  HWND hwnd = CreateWindowExW(0, kClassName, L"TamaPoke", WS_OVERLAPPED | WS_CAPTION |
    WS_SYSMENU | WS_MINIMIZEBOX, CW_USEDEFAULT, CW_USEDEFAULT, r.right-r.left, r.bottom-r.top,
    nullptr, nullptr, instance, nullptr);
  if (!hwnd) return 1;
  ShowWindow(hwnd, show); UpdateWindow(hwnd);
  MSG msg{};
  while (GetMessageW(&msg, nullptr, 0, 0) > 0) { TranslateMessage(&msg); DispatchMessageW(&msg); }
  return static_cast<int>(msg.wParam);
}