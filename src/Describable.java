// 설명 능력 인터페이스 — 거래가 Transaction(실행 능력)과 함께 동시에 구현한다.
//   · default 메서드로 기본 동작을 제공 → 여러 구현이 행위를 물려받는 "자바식 다중 상속".
//   · 한 객체를 Transaction으로도 Describable으로도 다룰 수 있게 한다(다중 인터페이스 구현).
interface Describable {
    default String describe() {
        return "거래";
    }
}
