import styles from "./scan-visual.module.css";

interface ScanVisualProps {
  className?: string;
}

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
