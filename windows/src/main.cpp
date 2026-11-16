#define UNICODE
#define _UNICODE
#include <windows.h>
#include <ctime>
#include <string>
#include "Arduino.h"
#include "pet.h"
#include "dex.h"

namespace {
constexpr wchar_t kClassName[] = L"TamaPokeDesktopWindow";
constexpr int W=720, H=780;
Pet pet;
int page=0;
int dexPage=0;
time_t lastClockSync=0;
std::wstring notice=L"반가워요! 타마포크를 시작해 보세요.";

COLORREF rgb565(uint16_t c) {
  int r=(c>>11)&31, g=(c>>5)&63, b=c&31;
  return RGB(r*255/31,g*255/63,b*255/31);
}
void text(HDC dc,const std::wstring&s,RECT r,int size,COLORREF color,bool bold=false) {
  HFONT f=CreateFontW(size,0,0,0,bold?FW_BOLD:FW_NORMAL,FALSE,FALSE,FALSE,
    DEFAULT_CHARSET,OUT_DEFAULT_PRECIS,CLIP_DEFAULT_PRECIS,CLEARTYPE_QUALITY,
    DEFAULT_PITCH,L"Malgun Gothic");
  auto old=SelectObject(dc,f); SetBkMode(dc,TRANSPARENT); SetTextColor(dc,color);
  DrawTextW(dc,s.c_str(),-1,&r,DT_CENTER|DT_VCENTER|DT_SINGLELINE|DT_END_ELLIPSIS);
  SelectObject(dc,old); DeleteObject(f);
}
void fillRound(HDC dc,RECT r,COLORREF fill,COLORREF edge,int radius=18) {
  HBRUSH b=CreateSolidBrush(fill); HPEN p=CreatePen(PS_SOLID,2,edge);
  auto ob=SelectObject(dc,b); auto op=SelectObject(dc,p);
  RoundRect(dc,r.left,r.top,r.right,r.bottom,radius,radius);
  SelectObject(dc,ob); SelectObject(dc,op); DeleteObject(b); DeleteObject(p);
}
void button(HDC dc,int x,int y,int w,int h,const wchar_t* label,bool active=false) {
  fillRound(dc,RECT{x,y,x+w,y+h},active?RGB(255,224,170):RGB(255,250,241),
            active?RGB(205,135,65):RGB(218,198,169),16);
  text(dc,label,RECT{x+4,y+2,x+w-4,y+h-2},18,RGB(82,64,51),true);
}
void bar(HDC dc,int x,int y,const wchar_t* label,uint8_t value,COLORREF tint) {
  text(dc,label,RECT{x,y,x+90,y+26},15,RGB(83,72,62),true);
  RECT track{x+92,y+5,x+380,y+23}; fillRound(dc,track,RGB(226,218,204),RGB(226,218,204),10);
  int width=(track.right-track.left)*value/100;
  if(width>0){RECT f{track.left,track.top,track.left+width,track.bottom};fillRound(dc,f,tint,tint,10);}
  text(dc,std::to_wstring(value),RECT{x+390,y,x+435,y+26},14,RGB(83,72,62),true);
}
std::wstring petName() {
  if(pet.isEgg()) return L"알";
  if(pet.nick[0]) return std::wstring(pet.nick,pet.nick+strlen(pet.nick));
  return std::wstring(DEX_TBL[pet.speciesId].name,DEX_TBL[pet.speciesId].name+strlen(DEX_TBL[pet.speciesId].name));
}
void drawPet(HDC dc) {
  COLORREF accent=pet.isEgg()?RGB(248,220,155):rgb565(DEX_TBL[pet.speciesId].accent);
  HBRUSH shadow=CreateSolidBrush(RGB(28,31,42)); HPEN rim=CreatePen(PS_SOLID,9,RGB(204,164,112));
  auto ob=SelectObject(dc,shadow);auto op=SelectObject(dc,rim);Ellipse(dc,210,92,510,392);
  SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(shadow);DeleteObject(rim);
  HBRUSH inner=CreateSolidBrush(RGB(43,50,66));HPEN no=CreatePen(PS_NULL,0,RGB(43,50,66));
  ob=SelectObject(dc,inner);op=SelectObject(dc,no);Ellipse(dc,224,106,496,378);
  SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(inner);DeleteObject(no);
  HBRUSH body=CreateSolidBrush(accent);HPEN outline=CreatePen(PS_SOLID,3,RGB(75,62,53));
  ob=SelectObject(dc,body);op=SelectObject(dc,outline);
  if(pet.isEgg()) Ellipse(dc,300,185,420,315); else Ellipse(dc,286,175,434,320);
  SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(body);DeleteObject(outline);
  if(pet.isEgg()) {
    text(dc,L"?",RECT{320,205,400,285},52,RGB(110,85,55),true);
  } else {
    HBRUSH eye=CreateSolidBrush(RGB(35,38,46)); ob=SelectObject(dc,eye);
    Ellipse(dc,326,220,340,239);Ellipse(dc,382,220,396,239);
    SelectObject(dc,ob);DeleteObject(eye);
    HPEN smile=CreatePen(PS_SOLID,3,RGB(90,57,45));op=SelectObject(dc,smile);
    Arc(dc,345,240,380,270,345,252,380,252);SelectObject(dc,op);DeleteObject(smile);
  }
  if(pet.showHeart()) text(dc,L"♥",RECT{410,145,470,205},36,RGB(255,130,150),true);
  if(pet.eating()) text(dc,L"냠!",RECT{250,145,310,190},22,RGB(255,226,150),true);
}
void drawHome(HDC dc) {
  drawPet(dc);
  text(dc,petName(),RECT{180,395,540,430},24,RGB(75,61,49),true);
  std::wstring meta=pet.isEgg()?L"새로운 동료를 기다리는 중":L"레벨 "+std::to_wstring(pet.level())+L"  ·  함께한 시간 "+std::to_wstring(pet.ageMinutes/60)+L"시간";
  text(dc,meta,RECT{120,428,600,458},15,RGB(125,105,86));
  bar(dc,142,470,L"배고픔",pet.fullness,RGB(237,166,75));
  bar(dc,142,503,L"기분",pet.joy,RGB(236,130,158));
  bar(dc,142,536,L"에너지",pet.energy,RGB(105,168,220));
  bar(dc,142,569,L"청결",pet.hygiene,RGB(100,185,145));
  if(pet.awaitingStarter()) {
    text(dc,L"첫 파트너를 선택하세요",RECT{90,615,630,648},18,RGB(95,73,52),true);
    button(dc,88,658,170,54,L"이상해씨");
    button(dc,275,658,170,54,L"파이리");
    button(dc,462,658,170,54,L"꼬부기");
  } else if(pet.isEgg()) {
    text(dc,L"알을 클릭해 부화시켜 보세요 (3회)",RECT{90,615,630,648},17,RGB(95,73,52),true);
    button(dc,250,658,220,54,L"알 톡톡!");
  } else {
    button(dc,44,625,145,54,L"먹이 주기");
    button(dc,202,625,145,54,L"놀아주기");
    button(dc,360,625,145,54,L"목욕시키기");
    button(dc,518,625,158,54,pet.sleeping?L"깨우기":L"재우기");
    if(pet.wantEvolveButton()) button(dc,250,690,220,44,L"진화하기",true);
    else if(pet.wantFarewellButton()) button(dc,250,690,220,44,L"작별 인사",true);
    else if(pet.canRunawayNow()) text(dc,L"돌봄이 필요해요!",RECT{200,690,520,730},16,RGB(180,75,65),true);
  }
}
void drawStats(HDC dc) {
  text(dc,L"상태 정보",RECT{100,105,620,150},27,RGB(76,60,47),true);
  text(dc,petName()+L"  ·  Lv."+std::to_wstring(pet.level()),RECT{100,160,620,198},21,RGB(88,71,56),true);
  const std::wstring rows[]={L"공격력  "+std::to_wstring(pet.atkStat()),L"방어력  "+std::to_wstring(pet.defStat()),L"속도  "+std::to_wstring(pet.speStat()),L"몸무게  "+std::to_wstring(pet.weight),L"유대감  "+std::to_wstring(pet.bond),L"돌봄 실수  "+std::to_wstring(pet.careMistakes)};
  for(int i=0;i<6;i++) text(dc,rows[i],RECT{150,230+i*55,570,270+i*55},20,RGB(83,68,55));
  text(dc,L"훈련은 아래 버튼에서 진행할 수 있습니다.",RECT{100,590,620,630},15,RGB(120,100,80));
  button(dc,240,650,240,58,L"힘 훈련하기");
}
void drawDex(HDC dc) {
  text(dc,L"포켓몬 도감",RECT{100,100,620,150},27,RGB(76,60,47),true);
  text(dc,L"등록 "+std::to_wstring(pet.registeredCount())+L" / 151",RECT{100,150,620,190},20,RGB(88,71,56),true);
  int start=dexPage*24+1;
  for(int i=0;i<24;i++){
    int id=start+i;if(id>151)break;
    int col=i%4,row=i/4,x=48+col*158,y=215+row*62;
    bool seen=pet.isRegistered(id);
    fillRound(dc,RECT{x,y,x+145,y+50},seen?RGB(255,249,236):RGB(235,231,222),RGB(216,204,184),10);
    std::wstring name=seen?std::wstring(DEX_TBL[id].name,DEX_TBL[id].name+strlen(DEX_TBL[id].name)):L"???";
    text(dc,std::to_wstring(id)+L". "+name,RECT{x+4,y+3,x+141,y+47},13,seen?RGB(75,62,51):RGB(155,148,137),seen);
  }
  button(dc,240,650,105,48,L"이전");
  text(dc,std::to_wstring(dexPage+1)+L" / 7",RECT{350,650,430,698},16,RGB(90,75,60),true);
  button(dc,440,650,105,48,L"다음");
}
void paint(HWND hwnd) {
  PAINTSTRUCT ps{};HDC dc=BeginPaint(hwnd,&ps);
  RECT client{};GetClientRect(hwnd,&client);
  HBRUSH bg=CreateSolidBrush(RGB(248,242,231));FillRect(dc,&client,bg);DeleteObject(bg);
  text(dc,L"TamaPoke",RECT{20,15,700,62},32,RGB(79,62,49),true);
  text(dc,L"나만의 포켓몬을 돌보는 작은 데스크톱 게임",RECT{20,60,700,88},14,RGB(133,112,91));
  if(page==0)drawHome(dc);else if(page==1)drawStats(dc);else drawDex(dc);
  fillRound(dc,RECT{38,730,682,770},RGB(238,228,211),RGB(224,210,188),12);
  text(dc,notice,RECT{50,733,670,767},14,RGB(105,84,65));
  button(dc,85,790,150,48,L"돌보기",page==0);
  button(dc,285,790,150,48,L"상태",page==1);
  button(dc,485,790,150,48,L"도감",page==2);
  EndPaint(hwnd,&ps);
}
void act(HWND hwnd,int x,int y) {
  if(y>=790&&y<=838){page=x<260?0:x<460?1:2;InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page==2&&y>=640&&y<=710){if(x<360&&dexPage>0)dexPage--;else if(x>430&&dexPage<6)dexPage++;InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page==1&&y>=640&&y<=715){uint8_t gain=pet.trainStrength(24);notice=L"힘 훈련 완료! 상승량 "+std::to_wstring(gain);InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page!=0)return;
  if(pet.awaitingStarter()&&y>=650&&y<=720){
    int id=x<265?1:x<455?4:7;pet.chooseStarter(id);pet.eggTap();notice=L"첫 파트너를 선택했어요!";InvalidateRect(hwnd,nullptr,FALSE);return;
  }
  if(pet.isEgg()&&y>=100&&y<=400){pet.eggTap();notice=L"알을 톡톡 두드렸어요!";InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(y>=620&&y<=685){
    if(x<195){pet.feedBerry(0);notice=L"맛있는 베리를 줬어요!";}
    else if(x<352){pet.playResult(5);pet.caress();notice=L"함께 놀았어요!";}
    else if(x<510){pet.clean();notice=L"깨끗하게 목욕했어요!";}
    else {pet.toggleLight();notice=pet.sleeping?L"잘 자요!":L"일어났어요!";}
    InvalidateRect(hwnd,nullptr,FALSE);return;
  }
  if(y>=685&&y<=730&&x>=240&&x<=480){
    if(pet.wantEvolveButton()){pet.evolve();notice=L"진화가 시작됐어요!";}
    else if(pet.wantFarewellButton()){pet.startFarewell();notice=L"작별 인사를 시작했어요.";}
    InvalidateRect(hwnd,nullptr,FALSE);return;
  }
  if(y>=100&&y<=400){pet.caress();notice=L"쓰다듬어 줬어요!";InvalidateRect(hwnd,nullptr,FALSE);}
}
LRESULT CALLBACK proc(HWND hwnd,UINT msg,WPARAM wp,LPARAM lp){
  switch(msg){
    case WM_CREATE:{pet.begin();std::srand((unsigned)std::time(nullptr));time_t now=std::time(nullptr);pet.syncClock((uint32_t)now);lastClockSync=now;SetTimer(hwnd,1,250,nullptr);return 0;}
    case WM_TIMER:{pet.update(millis());time_t now=std::time(nullptr);if(now-lastClockSync>=60){pet.setClock((uint32_t)now);lastClockSync=now;}if(pet.savePending())pet.flushSave();InvalidateRect(hwnd,nullptr,FALSE);return 0;}
    case WM_LBUTTONUP:act(hwnd,LOWORD(lp),HIWORD(lp));return 0;
    case WM_PAINT:paint(hwnd);return 0;
    case WM_ERASEBKGND:return 1;
    case WM_DESTROY:KillTimer(hwnd,1);pet.setClock((uint32_t)std::time(nullptr));PostQuitMessage(0);return 0;
    default:return DefWindowProcW(hwnd,msg,wp,lp);
  }
}
}
int WINAPI wWinMain(HINSTANCE instance,HINSTANCE,PWSTR,int show){
  WNDCLASSW wc{};wc.lpfnWndProc=proc;wc.hInstance=instance;wc.lpszClassName=kClassName;
  wc.hCursor=LoadCursor(nullptr,IDC_HAND);wc.hbrBackground=(HBRUSH)(COLOR_WINDOW+1);
  if(!RegisterClassW(&wc))return 1;
  RECT r{0,0,W,H};AdjustWindowRect(&r,WS_OVERLAPPED|WS_CAPTION|WS_SYSMENU|WS_MINIMIZEBOX,FALSE);
  HWND hwnd=CreateWindowExW(0,kClassName,L"TamaPoke",WS_OVERLAPPED|WS_CAPTION|WS_SYSMENU|WS_MINIMIZEBOX,
    CW_USEDEFAULT,CW_USEDEFAULT,r.right-r.left,r.bottom-r.top,nullptr,nullptr,instance,nullptr);
  if(!hwnd)return 1;ShowWindow(hwnd,show);UpdateWindow(hwnd);
  MSG msg{};while(GetMessageW(&msg,nullptr,0,0)>0){TranslateMessage(&msg);DispatchMessageW(&msg);}
  return (int)msg.wParam;
}
