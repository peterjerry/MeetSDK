#ifndef _COMMON_H_
#define _COMMON_H_

#include "apCatalog.h"
#include "apContent.h"
#include "apModule.h"
#include "apNavigator.h"
#include "apPlayLink2.h"

#include <vector>
#include <map>

typedef std::map<std::string, std::string> MAP_ITEM;

typedef std::vector<apCatalog>		EPG_CATALOG_LIST;
typedef std::vector<apContent>		EPG_CONTENT_LIST;
typedef std::vector<apModule>		EPG_MODULE_LIST;
typedef std::vector<apNavigator>	EPG_NAVIGATOR_LIST;
typedef std::vector<apPlayLink2>	EPG_PLAYLINK_LIST;

#endif // _COMMON_H_