import styles from "./scan-visual.module.css";

/**
 * ScanVisual 컴포넌트의 속성입니다.
 */
interface ScanVisualProps {
  className?: string;
}

/**
 * 시각적 스캐닝 애니메이션 컴포넌트입니다.
 * 애니메이션 격자 및 선이 있는 스캐닝 프레임을 표시합니다.
 *
 * @param props - 컴포넌트 속성
 * @param props.className - 추가 CSS 클래스
 * @returns 스캔 시각 컴포넌트
 */
export default function ScanVisual({ className = "" }: ScanVisualProps) {
  return (
    <div className={`${styles['scan-frame']} ${className}`}>
      <div className={styles['scan-grid']} />
      <div className={styles['scan-line']} />
      <div className={`${styles['scan-corner']} ${styles.tl}`} />
      <div className={`${styles['scan-corner']} ${styles.tr}`} />
      <div className={`${styles['scan-corner']} ${styles.bl}`} />
      <div className={`${styles['scan-corner']} ${styles.br}`} />
      <span className={styles['scan-tag']}>SLICE 084 / 220</span>
    </div>
  );
}
