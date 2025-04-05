#include "Optionometer.h"

const auto DEFAULT_TICKER = "QQQ";

int main(const int argc, const char* argv[]) {
  const auto ticker = (argc > 1) ? argv[1] : DEFAULT_TICKER;
  std::cout << "Optionometer started with ticker " << ticker << std::endl;
  return 0;
}
