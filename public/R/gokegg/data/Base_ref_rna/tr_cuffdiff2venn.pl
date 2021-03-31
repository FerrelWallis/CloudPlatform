#!usr/bin/perl -w

use strict;
use FindBin qw($Bin $Script);
die "cuffdiff format\nsam1\tpath1\nsam2\tpath2\nusage:perl $0 <diff.anno list> <output-name> \n" unless @ARGV == 2;

open A,$ARGV[0];
my @lab;

open A,$ARGV[0];my %h;my %anno;my %h2;my @sam;

my $record;
while(<A>){
    chomp;my @c=split;
    push @sam,$c[0];$record=$.;
    open IN,$c[1];<IN>;my $rec=0;
    while(my $inf=<IN>){
        chomp $inf;my @a=split /\s+/,$inf;
        $h{$a[0]}[1]++;
        $h{$a[0]}[0]=$rec;
        push @{$h2{$a[0]}},$record;
        $rec++;
    }
    close IN;
    my $recc=0;
    foreach my $k1(sort keys %h){
        if($h{$k1}[1] > 1){
            $h{$k1}[0]=$recc;
            $recc++;
        }
    }
}
close A;
open A,$ARGV[0];
while(<A>){
    chomp;my $rec=$.;my @cc=split;  open IN,$cc[1];my %tar;
    my $head=<IN>;open OUT,">$ARGV[1]$rec.imp";my $id=$ARGV[1].$rec.".imp";push @lab,$id;
    open OUT2,">$ARGV[1].$cc[0].overlap.detail.xls";
    print OUT2 "1:$sam[0]";
    foreach my $k_s(1..$#sam){
        my $rec=$k_s+1;
        print OUT2 "\t$rec:$sam[$k_s]";
    }
    print OUT2 "\ntype\toverlap-num\toverlap-detail\t$head";
    while(my $inf=<IN>){
        chomp $inf;my @a=split /\s+/,$inf;
        if($h{$a[0]}[1] > 1){
            print OUT "tmp_$h{$a[0]}[0]\n";
            my $part=join ",",@{$h2{$a[0]}};
            print OUT2 "overlap\t$h{$a[0]}[1]\t$part\t$inf\n";
        }
        else{
            $tar{$a[0]}=1;
            print OUT2 "special\t$h{$a[0]}[1]\t$h2{$a[0]}[0]\t$inf\n";
        }
    }
    my @site=sort keys %tar;
    foreach my $k1(0..$#site){
        print OUT "$lab[$rec-1]_$k1\n";
    }
    close OUT;close IN;close OUT2;
}
close A;
my $lab=join ",",@lab;  my $sam2=join ",",@sam;
`perl $Bin/Venn-RNAseq.pl -f $lab -l $sam2`;
