#pragma once

#ifdef new
#undef new
#endif

#include <functional>
using std::less;
using std::less_equal;
using std::greater;
using std::equal_to;
using std::hash;
using std::function;
using std::ref;

#ifdef DEBUG_NEW
#define new DEBUG_NEW
#endif
