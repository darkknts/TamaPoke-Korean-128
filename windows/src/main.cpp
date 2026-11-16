#define UNICODE
#define _UNICODE
#include <windows.h>
#include <ctime>
#include <string>
#include <cstdlib>
#include <fstream>
#include <vector>
#include <filesystem>
#include <algorithm>
#include <cstring>
#include <iterator>
#include "audio.h"
#include "Arduino.h"
#include "pet.h"
#include "dex.h"
#include "species.h"

namespace {
constexpr wchar_t kClassName[] = L"TamaPokeDesktopWindow";
constexpr int W=720, H=860;
Pet pet;
int page=0;
int dexPage=0;
bool gameActive=false; DWORD gameStarted=0; int gameScore=0; int targetX=360,targetY=330;
time_t lastClockSync=0;
struct DesktopSprite { int w=0,h=0; std::vector<uint16_t> pixels; std::vector<uint8_t> opaque; std::vector<uint16_t> durations; uint32_t totalDuration=0; };
std::vector<DesktopSprite> desktopSprites(152);
uint16_t read16(const std::vector<unsigned char>& b,size_t p){return (uint16_t)(b[p]|(b[p+1]<<8));}
uint32_t read32(const std::vector<unsigned char>& b,size_t p){return (uint32_t)b[p]|((uint32_t)b[p+1]<<8)|((uint32_t)b[p+2]<<16)|((uint32_t)b[p+3]<<24);}
void loadDesktopSprites() {
  wchar_t exe[MAX_PATH]{}; GetModuleFileNameW(nullptr,exe,MAX_PATH);
  std::filesystem::path packPath(exe); packPath=packPath.parent_path()/L"sprites.pak";
  std::ifstream in(packPath,std::ios::binary); if(!in)return;
  std::vector<unsigned char> b((std::istreambuf_iterator<char>(in)),{});
  if(b.size()<6||memcmp(b.data(),"TPAK",4)!=0)return;
  uint16_t count=read16(b,4); size_t p=6;
  struct Entry{std::string name;uint32_t size;};std::vector<Entry> entries;entries.reserve(count);
  for(uint16_t i=0;i<count;i++){
    if(p>=b.size())return;uint8_t n=b[p++];if(p+n+4>b.size())return;
    std::string name((char*)b.data()+p,n);p+=n;uint32_t size=read32(b,p);p+=4;entries.push_back({name,size});
  }
  for(const auto& e:entries){
    if(p+e.size>b.size())return;
    if(e.name.rfind("mons/p",0)==0&&e.name.size()==13&&e.name.substr(9)==".bin"){
      int dex=std::atoi(e.name.substr(6,3).c_str());
      if(dex>=1&&dex<=151&&e.size>=7&&memcmp(b.data()+p,"TPK2",4)==0){
        size_t q=p;uint8_t acts=b[q+4];uint16_t colors=read16(b,q+5);q+=7;
        if(q+(size_t)colors*2<=p+e.size){
          std::vector<uint16_t> pal(colors);for(uint16_t k=0;k<colors;k++)pal[k]=read16(b,q+k*2);q+=(size_t)colors*2;
          for(uint8_t a=0;a<acts&&q+4<=p+e.size;a++){
            uint8_t id=b[q],w=b[q+1],h=b[q+2],frames=b[q+3];q+=4;
            if(!w||!h||!frames||q+(size_t)frames*2+(size_t)w*h*frames>p+e.size)break;
            size_t frameBytes=(size_t)w*h;
            if(id==0){
              DesktopSprite sp;sp.w=w;sp.h=h;
              sp.durations.reserve(frames);
              for(uint8_t frame=0;frame<frames;frame++){
                uint16_t duration=read16(b,q+(size_t)frame*2);
                sp.durations.push_back(duration?duration:100);
                sp.totalDuration+=sp.durations.back();
              }
              q+=(size_t)frames*2;
              sp.pixels.resize(frameBytes*frames);sp.opaque.resize(frameBytes*frames);
              for(size_t z=0;z<frameBytes*frames;z++){
                uint8_t ix=b[q+z];
                sp.opaque[z]=(ix!=255&&ix<pal.size())?1:0;
                sp.pixels[z]=sp.opaque[z]?pal[ix]:0;
              }
              desktopSprites[dex]=std::move(sp);
              q+=frameBytes*frames;
              continue;
            }
            q+=(size_t)frames*2+frameBytes*frames;
          }
        }
      }
    }
    p+=e.size;
  }
}
std::wstring notice=L"반가워요! 타마포크를 시작해 보세요.";

COLORREF rgb565(uint16_t c);
void drawDesktopSprite(HDC dc,int dex,RECT bounds,DWORD animationClock=0) {
  if(dex<1||dex>151)return;
  const auto& sp=desktopSprites[dex];
  if(sp.w<=0||sp.h<=0||sp.durations.empty()||sp.pixels.size()!=(size_t)sp.w*sp.h*sp.durations.size()||sp.opaque.size()!=sp.pixels.size())return;
  size_t frame=0;
  if(sp.totalDuration){uint32_t tick=animationClock%sp.totalDuration;while(frame+1<sp.durations.size()&&tick>=sp.durations[frame]){tick-=sp.durations[frame];frame++;}}
  const int bw=bounds.right-bounds.left,bh=bounds.bottom-bounds.top;
  if(bw<=0||bh<=0)return;
  const double scale=std::min((double)bw/sp.w,(double)bh/sp.h);
  const int dw=std::max(1,(int)(sp.w*scale)),dh=std::max(1,(int)(sp.h*scale));
  const int ox=bounds.left+(bw-dw)/2,oy=bounds.top+(bh-dh)/2;
  for(int y=0;y<dh;y++){
    int sy=std::min(sp.h-1,(int)((double)y*sp.h/dh));
    for(int x=0;x<dw;x++){
      int sx=std::min(sp.w-1,(int)((double)x*sp.w/dw));
      size_t index=frame*(size_t)sp.w*sp.h+(size_t)sy*sp.w+sx;
      if(!sp.opaque[index])continue;
      HBRUSH px=CreateSolidBrush(rgb565(sp.pixels[index]));
      RECT pr{ox+x,oy+y,ox+x+1,oy+y+1};
      FillRect(dc,&pr,px);DeleteObject(px);
    }
  }
}

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
std::wstring fromUtf8(const char* value) {
  if (!value || !*value) return L"";
  int n=MultiByteToWideChar(CP_UTF8,0,value,-1,nullptr,0);
  if(n<=1) return L"";
  std::wstring out(n,L'\\0'); MultiByteToWideChar(CP_UTF8,0,value,-1,out.data(),n); out.resize(n-1);
  return out;
}
std::wstring petName() {
  if(pet.isEgg()) return L"알";
  if(pet.nick[0]) return fromUtf8(pet.nick);
  return fromUtf8(DEX_TBL[pet.speciesId].name);
}
void drawPet(HDC dc) {
  COLORREF accent=pet.isEgg()?RGB(248,220,155):rgb565(DEX_TBL[pet.speciesId].accent);
  HBRUSH shadow=CreateSolidBrush(RGB(28,31,42)); HPEN rim=CreatePen(PS_SOLID,9,RGB(204,164,112));
  auto ob=SelectObject(dc,shadow);auto op=SelectObject(dc,rim);Ellipse(dc,210,92,510,392);
  SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(shadow);DeleteObject(rim);
  HBRUSH inner=CreateSolidBrush(RGB(43,50,66));HPEN no=CreatePen(PS_NULL,0,RGB(43,50,66));
  ob=SelectObject(dc,inner);op=SelectObject(dc,no);Ellipse(dc,224,106,496,378);
  SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(inner);DeleteObject(no);
  const char* const* sprite=nullptr;
  if(pet.isEgg()) sprite=SPR_EGG;
  else if(pet.speciesId>=1&&pet.speciesId<=151&&desktopSprites[pet.speciesId].w>0) {
    int saved=SaveDC(dc); HRGN mask=CreateEllipticRgn(224,106,496,378); SelectClipRgn(dc,mask);
    drawDesktopSprite(dc,pet.speciesId,RECT{245,130,475,360},GetTickCount());
    RestoreDC(dc,saved); DeleteObject(mask);
    if(pet.showHeart()) text(dc,L"♥",RECT{410,145,470,205},36,RGB(255,130,150),true);
    if(pet.eating()) text(dc,L"냠!",RECT{250,145,310,190},22,RGB(255,226,150),true);
    return;
  } else {
    int id=pet.speciesId;
    if(id>=1&&id<=3)sprite=SPECIES[SP_BULBASAUR+(id-1)].sprite;
    else if(id>=4&&id<=6)sprite=SPECIES[SP_CHARMANDER+(id-4)].sprite;
    else if(id>=7&&id<=9)sprite=SPECIES[SP_SQUIRTLE+(id-7)].sprite;
  }
  if(sprite) {
    const int scale=5, sx=280, sy=155;
    for(int row=0;row<32;row++) for(int col=0;col<32;col++) {
      uint16_t color=spriteColor(sprite[row][col]);
      if(color) {
        HBRUSH px=CreateSolidBrush(rgb565(color));
        RECT pr{sx+col*scale,sy+row*scale,sx+(col+1)*scale,sy+(row+1)*scale};
        FillRect(dc,&pr,px);DeleteObject(px);
      }
    }
  } else {
    HBRUSH body=CreateSolidBrush(accent);HPEN outline=CreatePen(PS_SOLID,3,RGB(75,62,53));
    ob=SelectObject(dc,body);op=SelectObject(dc,outline);Ellipse(dc,286,175,434,320);
    SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(body);DeleteObject(outline);
    HBRUSH eye=CreateSolidBrush(RGB(35,38,46));ob=SelectObject(dc,eye);
    Ellipse(dc,326,220,340,239);Ellipse(dc,382,220,396,239);
    SelectObject(dc,ob);DeleteObject(eye);
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
int gameSecondsLeft(){DWORD elapsed=GetTickCount()-gameStarted;return elapsed>=15000?0:(int)((15000-elapsed+999)/1000);}
void drawMore(HDC dc) {
  text(dc,L"더보기",RECT{100,105,620,150},27,RGB(76,60,47),true);
  text(dc,L"미니게임 · 설정 · 포켓몬 이름",RECT{100,155,620,190},16,RGB(120,100,80));
  if(gameActive) {
    text(dc,L"타이밍 캐치  ·  남은 시간 "+std::to_wstring(gameSecondsLeft())+L"초",RECT{90,215,630,255},20,RGB(76,60,47),true);
    text(dc,L"점수 "+std::to_wstring(gameScore),RECT{90,265,630,300},18,RGB(88,71,56),true);
    HBRUSH b=CreateSolidBrush(RGB(237,166,75)); HPEN p=CreatePen(PS_SOLID,3,RGB(160,110,55));
    auto ob=SelectObject(dc,b);auto op=SelectObject(dc,p);Ellipse(dc,targetX-30,targetY-30,targetX+30,targetY+30);
    SelectObject(dc,ob);SelectObject(dc,op);DeleteObject(b);DeleteObject(p);
    text(dc,L"원을 빠르게 클릭하세요!",RECT{120,500,600,540},18,RGB(95,73,52),true);
  } else {
    button(dc,180,235,360,58,L"미니게임 시작",true);
    button(dc,180,315,360,58,L"포켓몬 이름 변경");
    button(dc,180,395,360,58,audioEnabled()?L"효과음: 켜짐":L"효과음: 꺼짐");
    text(dc,L"15초 동안 움직이는 표적을 클릭해 점수를 얻으세요.",RECT{90,485,630,530},15,RGB(120,100,80));
    text(dc,L"효과음 설정은 다음 실행에도 유지됩니다.",RECT{90,535,630,575},14,RGB(120,100,80));
  }
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
    if(seen) drawDesktopSprite(dc,id,RECT{x+5,y+5,x+48,y+47},GetTickCount());
    else {
      HBRUSH silhouette=CreateSolidBrush(RGB(210,205,195));
      HBRUSH oldBrush=(HBRUSH)SelectObject(dc,silhouette);
      Ellipse(dc,x+15,y+12,x+38,y+39);
      SelectObject(dc,oldBrush);DeleteObject(silhouette);
    }
    text(dc,std::to_wstring(id)+L". "+name,RECT{x+50,y+3,x+141,y+47},12,seen?RGB(75,62,51):RGB(155,148,137),seen);
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
  if(page==0)drawHome(dc);else if(page==1)drawStats(dc);else if(page==2)drawDex(dc);else drawMore(dc);
  fillRound(dc,RECT{38,730,682,770},RGB(238,228,211),RGB(224,210,188),12);
  text(dc,notice,RECT{50,733,670,767},14,RGB(105,84,65));
  button(dc,28,790,145,48,L"돌보기",page==0);
  button(dc,190,790,145,48,L"상태",page==1);
  button(dc,352,790,145,48,L"도감",page==2);
  button(dc,514,790,178,48,L"더보기",page==3);
  EndPaint(hwnd,&ps);
}
LRESULT CALLBACK renameProc(HWND h,UINT m,WPARAM w,LPARAM l) {
  static HWND edit=nullptr;
  switch(m) {
    case WM_CREATE: {
      edit=CreateWindowExW(WS_EX_CLIENTEDGE,L"EDIT",petName().c_str(),WS_CHILD|WS_VISIBLE|ES_AUTOHSCROLL,22,22,310,32,h,(HMENU)101,GetModuleHandleW(nullptr),nullptr);
      CreateWindowW(L"BUTTON",L"저장",WS_CHILD|WS_VISIBLE|BS_DEFPUSHBUTTON,160,72,80,34,h,(HMENU)IDOK,GetModuleHandleW(nullptr),nullptr);
      CreateWindowW(L"BUTTON",L"취소",WS_CHILD|WS_VISIBLE,250,72,80,34,h,(HMENU)IDCANCEL,GetModuleHandleW(nullptr),nullptr);
      SetFocus(edit); SendMessageW(edit,EM_SETSEL,0,-1); return 0;
    }
    case WM_COMMAND:
      if(LOWORD(w)==IDOK) {
        wchar_t wide[64]{}; GetWindowTextW(edit,wide,64);
        char utf[64]{}; int n=WideCharToMultiByte(CP_UTF8,0,wide,-1,utf,sizeof(utf),nullptr,nullptr);
        if(n>0&&n<=12&&wide[0]) {pet.rename(utf);DestroyWindow(h);}
        else MessageBoxW(h,L"이름은 UTF-8 기준 11바이트 이내로 입력해 주세요.",L"TamaPoke",MB_OK|MB_ICONINFORMATION);
        return 0;
      }
      if(LOWORD(w)==IDCANCEL){DestroyWindow(h);return 0;} break;
    case WM_CLOSE: DestroyWindow(h);return 0;
  }
  return DefWindowProcW(h,m,w,l);
}
void renamePet(HWND owner) {
  static bool registered=false;
  if(!registered){WNDCLASSW wc{};wc.lpfnWndProc=renameProc;wc.hInstance=GetModuleHandleW(nullptr);wc.lpszClassName=L"TamaPokeRenameDialog";wc.hCursor=LoadCursor(nullptr,IDC_IBEAM);wc.hbrBackground=(HBRUSH)(COLOR_WINDOW+1);RegisterClassW(&wc);registered=true;}
  HWND dlg=CreateWindowExW(WS_EX_DLGMODALFRAME,L"TamaPokeRenameDialog",L"포켓몬 이름 변경",WS_CAPTION|WS_SYSMENU|WS_POPUP|WS_VISIBLE,0,0,360,150,owner,nullptr,GetModuleHandleW(nullptr),nullptr);
  RECT wr{};GetWindowRect(owner,&wr);SetWindowPos(dlg,nullptr,wr.left+(wr.right-wr.left-360)/2,wr.top+(wr.bottom-wr.top-150)/2,0,0,SWP_NOSIZE|SWP_NOZORDER);
  EnableWindow(owner,FALSE); MSG msg{};while(IsWindow(dlg)&&GetMessageW(&msg,nullptr,0,0)>0){TranslateMessage(&msg);DispatchMessageW(&msg);}EnableWindow(owner,TRUE);SetActiveWindow(owner);
}
void act(HWND hwnd,int x,int y) {
  if(y>=790&&y<=838){page=x<180?0:x<342?1:x<505?2:3;InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page==3) {
    if(gameActive) {
      if(x>=targetX-42&&x<=targetX+42&&y>=targetY-42&&y<=targetY+42) {
        gameScore++; targetX=110+std::rand()%500; targetY=320+std::rand()%150;
      }
      InvalidateRect(hwnd,nullptr,FALSE);return;
    }
    if(y>=225&&y<=300) {gameActive=true;gameScore=0;gameStarted=GetTickCount();targetX=360;targetY=350;notice=L"표적을 클릭해 점수를 올려보세요!";InvalidateRect(hwnd,nullptr,FALSE);return;}
    if(y>=305&&y<=380) {renamePet(hwnd);InvalidateRect(hwnd,nullptr,FALSE);return;}
    if(y>=385&&y<=465) {audioSetEnabled(!audioEnabled());notice=audioEnabled()?L"효과음을 켰어요.":L"효과음을 껐어요.";InvalidateRect(hwnd,nullptr,FALSE);return;}
    return;
  }
  if(page==2&&y>=640&&y<=710){if(x<360&&dexPage>0)dexPage--;else if(x>430&&dexPage<6)dexPage++;InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page==1&&y>=640&&y<=715){uint8_t gain=pet.trainStrength(24);notice=L"힘 훈련 완료! 상승량 "+std::to_wstring(gain);InvalidateRect(hwnd,nullptr,FALSE);return;}
  if(page!=0)return;
  if(pet.awaitingStarter()&&y>=650&&y<=720){
    int id=x<265?1:x<455?4:7;pet.chooseStarter(id);notice=L"첫 파트너를 선택했어요! 이제 알을 세 번 톡톡 두드려 부화시켜 보세요." ;InvalidateRect(hwnd,nullptr,FALSE);return;
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
    else if(pet.wantFarewellButton()){if(MessageBoxW(hwnd,L"정말 작별 인사를 할까요?\n작별 후에는 새로운 알이 찾아옵니다.",L"TamaPoke",MB_YESNO|MB_ICONQUESTION|MB_DEFBUTTON2)==IDYES){pet.startFarewell();notice=L"작별 인사를 시작했어요.";}else{pet.declineFarewell();notice=L"조금 더 함께하기로 했어요.";}}
    InvalidateRect(hwnd,nullptr,FALSE);return;
  }
  if(y>=100&&y<=400){pet.caress();notice=L"쓰다듬어 줬어요!";InvalidateRect(hwnd,nullptr,FALSE);}
}
LRESULT CALLBACK proc(HWND hwnd,UINT msg,WPARAM wp,LPARAM lp){
  switch(msg){
    case WM_CREATE:{std::srand((unsigned)std::time(nullptr));audioBegin();loadDesktopSprites();pet.begin();time_t now=std::time(nullptr);pet.syncClock((uint32_t)now);lastClockSync=now;SetTimer(hwnd,1,250,nullptr);return 0;}
    case WM_TIMER:{pet.update(millis());if(gameActive&&GetTickCount()-gameStarted>=15000){gameActive=false;pet.playResult((uint8_t)(gameScore>255?255:gameScore));notice=L"미니게임 종료! 획득 점수: "+std::to_wstring(gameScore);page=3;}time_t now=std::time(nullptr);if(now-lastClockSync>=60){pet.setClock((uint32_t)now);lastClockSync=now;}if(pet.savePending())pet.flushSave();InvalidateRect(hwnd,nullptr,FALSE);return 0;}
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
