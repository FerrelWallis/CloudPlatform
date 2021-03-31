#!ust/bin/perl -w

use strict;

die "usage:perl $0 <snp.vcf> <output ID> <quality cutoff(20)> <low depth filter> <high depth filter>\n" unless @ARGV == 5;

open A,$ARGV[0];  open OUT1,">$ARGV[1]";
my %hs = (
    "AC" => 'M', "AG" => 'R',  "AT" => 'W',"CG" => 'S', "CT" => 'Y',  "GT" => 'K',
    "CA" => 'M', "GA" => 'R',  "TA" => 'W',"GC" => 'S', "TC" => 'Y',  "TG" => 'K',
    "ACG" => 'V',"ACT" => 'H', "AGT" => 'D',"CGT" => 'B',"ACGT" => 'X', '-' => "-",
);
my ($ts, $tv)=(0, 0);   my ($ts2, $tv2)=(0, 0); my %sam;    my %snp;    my %indel;  my @site;
#report snp info
print OUT1 "Chr\tPos\tPos\tRef\tALter\tSam-base\tDepth\tDepth(ref-forward,ref-reverse,alt-forward,alt-reverse)\n";
while(<A>){
    chomp;next if(/^#/);
    my @a=split;my ($dp, $varia)=(0, 0);
    my $len=length $a[4];   next if($len > 1);  my @c=split /[;=]/,$a[7];
    my @b=split /:/,$a[-1];
	next if($c[1] < $ARGV[3]);
	next if($c[1] > $ARGV[4]);
	next if($b[-1] < $ARGV[2]);
	my $dep4="_";
	if($a[7]=~/DP4=([0-9,]+);/){
		$dep4=$1;
	}
    if($b[0] eq "1/1"){
        print OUT1 "$a[0]\t$a[1]\t$a[1]\t$a[3]\t$a[4]\t$a[4]\t$c[1]\t$dep4\n";
    }
    else{
        my $id=$a[3].$a[4];
        print OUT1 "$a[0]\t$a[1]\t$a[1]\t$a[3]\t$a[4]\t$hs{$id}\t$c[1]\t$dep4\n";
    }
}
close A;close OUT1;
